import "dotenv/config";
import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";
import express from "express";
import cors from "cors";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import jwt from "jsonwebtoken";
import Database from "better-sqlite3";
import { migrateDatabase } from "./db/migrations.js";
import { migratePostgres } from "./db/postgres-migrations.js";
import { PostgresDatabase } from "./db/postgres-database.js";
import { SqliteCatalogueProvider, SqliteIdentityProvider, SqliteEntitlementProvider, UnconfiguredPlaybackProvider } from "./providers/sqlite-providers.js";
import { PostgresCatalogueProvider, PostgresIdentityProvider, PostgresEntitlementProvider, UnconfiguredPostgresPlaybackProvider } from "./providers/postgres-providers.js";
import { ProviderError, ProviderNotConfiguredError, ProviderAuthenticationError, ProviderAuthorizationError, ProviderContractError } from "./providers/errors.js";
import { DEMO_CATALOGUE, DemoEntitlementProvider, DemoPlaybackProvider, DemoLiveJoinProvider } from "./providers/demo-providers.js";
import { createDisabledRwaProviders } from "./providers/rwa/disabled-rwa-providers.js";

const validEmail = value => typeof value==="string" && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);

export function assertDatabaseConfiguration(env=process.env) {
  if(env.REQUIRE_POSTGRES==="true" && !env.DATABASE_URL)
    throw new Error("DATABASE_URL is required when REQUIRE_POSTGRES=true.");
  if(env.REQUIRE_POSTGRES==="true" && !(env.DATABASE_URL?.startsWith("postgres://") || env.DATABASE_URL?.startsWith("postgresql://")))
    throw new Error("A PostgreSQL DATABASE_URL is required for this deployment.");
}

export function createDatabase(filename=":memory:") {
  if(filename!==":memory:") fs.mkdirSync(path.dirname(path.resolve(filename)),{recursive:true});
  const db=new Database(filename);
  db.pragma("journal_mode = WAL");
  db.pragma("foreign_keys = ON");
  migrateDatabase(db);
  return db;
}

export async function createPostgresDatabase(connectionString=process.env.DATABASE_URL) {
  const db=PostgresDatabase.fromConnectionString(connectionString);
  await migratePostgres(db);
  return db;
}

const dbAll = (db,sql,...params) => db.dialect==="postgres" ? db.all(sql,...params) : db.prepare(sql).all(...params);
const dbGet = (db,sql,...params) => db.dialect==="postgres" ? db.get(sql,...params) : db.prepare(sql).get(...params);
const isUniqueViolation = error => error?.code==="23505" || String(error?.code||"").includes("SQLITE_CONSTRAINT");

export function createApp({db, jwtSecret=process.env.JWT_SECRET, providers={}, demoMode=process.env.DEMO_MODE==="true"}={}) {
  if(!db) throw new Error("A database connection is required.");
  if(typeof jwtSecret!=="string"||jwtSecret.length<32) throw new Error("JWT_SECRET must be configured with at least 32 characters.");
  const postgres=db.dialect==="postgres";
  const catalogue=providers.catalogue||(postgres?new PostgresCatalogueProvider(db):new SqliteCatalogueProvider(db));
  const identity=providers.identity||(postgres?new PostgresIdentityProvider(db,jwtSecret):new SqliteIdentityProvider(db,jwtSecret));
  const entitlement=providers.entitlement||(postgres?new PostgresEntitlementProvider(db):new SqliteEntitlementProvider(db));
  const playback=providers.playback||(postgres?new UnconfiguredPostgresPlaybackProvider():new UnconfiguredPlaybackProvider());
  // Demo providers are isolated from the real entitlement and playback providers.
  const demoEntitlement=demoMode?(providers.demoEntitlement||new DemoEntitlementProvider()):null;
  const demoPlayback=demoMode?(providers.demoPlayback||new DemoPlaybackProvider({entitlementProvider:demoEntitlement})):null;
  const demoLiveJoin=demoMode?(providers.demoLiveJoin||new DemoLiveJoinProvider({entitlementProvider:demoEntitlement})):null;
  const app=express();
  // Original RWA is opt-in only; incomplete or absent bundles always fail closed.
  const injectedRwa=providers.originalRwa;
  const originalRwaProviders=injectedRwa?.enabled===true && injectedRwa?.configurationStatus?.ready===true
    ? injectedRwa
    : createDisabledRwaProviders();
  app.locals.originalRwaProviders=originalRwaProviders;
  app.disable("x-powered-by");
  app.use(helmet());
  app.use(express.json({limit:"32kb"}));
  const origins=(process.env.CORS_ORIGINS||"").split(",").map(v=>v.trim()).filter(Boolean);
  app.use(cors({origin(origin,cb){if(!origin||origins.includes(origin))return cb(null,true);return cb(new Error("Origin is not allowed by CORS."));}}));
  const authLimiter=rateLimit({windowMs:15*60*1000,limit:10,standardHeaders:"draft-7",legacyHeaders:false});
  const requireAuth=(req,res,next)=>{
    const header=req.headers.authorization||"";
    if(!header.startsWith("Bearer "))return res.status(401).json({error:"Authentication required."});
    try { req.user=jwt.verify(header.slice(7),jwtSecret,{issuer:"balidan-rwa-api"}); next(); }
    catch { return res.status(401).json({error:"Invalid or expired access token."}); }
  };
  app.get("/health",async(_req,res)=>{
    try {
      await dbGet(db,"SELECT 1 AS ok");
      if(postgres) {
        const migration=await dbGet(db,"SELECT MAX(version) AS version FROM schema_migrations");
        return res.json({status:"ok",database:"postgres",migrations:Number(migration?.version||0)});
      }
      return res.json({status:"ok",database:"sqlite"});
    } catch {
      return res.status(503).json({status:"unavailable",database:postgres?"postgres":"sqlite"});
    }
  });
  app.post("/v1/auth/signup",authLimiter,async(req,res)=>{
    const {name,email,password}=req.body||{};
    if(typeof name!=="string"||name.trim().length<2||!validEmail(email)||typeof password!=="string"||password.length<10)return res.status(400).json({error:"Provide a name, valid email and password of at least 10 characters."});
    try{return res.status(201).json(await identity.register({name,email,password}));}
    catch(error){if(isUniqueViolation(error))return res.status(409).json({error:"An account with this email already exists."});return res.status(500).json({error:"Account could not be created."});}
  });
  app.post("/v1/auth/login",authLimiter,async(req,res)=>{
    const {email,password}=req.body||{};
    if(!validEmail(email)||typeof password!=="string")return res.status(400).json({error:"Email and password are required."});
    try { const result=await identity.authenticate({email,password}); return result?res.json(result):res.status(401).json({error:"Email or password is incorrect."}); }
    catch { return res.status(500).json({error:"Login could not be completed."}); }
  });
  // Demo fixtures and sessions are isolated from real provider records.
  app.get("/v1/demo/catalogue",(_req,res)=>{
    if(!demoMode)return res.status(404).json({error:"Demo mode is disabled."});
    return res.json({mode:"demo",notice:"Synthetic sample data and locally bundled media only. Not Original RWA content or authorization.",...DEMO_CATALOGUE});
  });
  app.get("/v1/demo/entitlements/content/:contentId",async(req,res)=>{
    if(!demoMode)return res.status(404).json({error:"Demo mode is disabled."});
    return res.json(await demoEntitlement.check("demo-user",req.params.contentId));
  });
  app.post("/v1/demo/playback/sessions",async(req,res,next)=>{
    if(!demoMode)return res.status(404).json({error:"Demo mode is disabled."});
    const contentId=req.body?.contentId;
    if(typeof contentId!=="string"||!contentId.trim())return res.status(400).json({error:"contentId is required."});
    try {
      const entitlementResult=await demoEntitlement.check("demo-user",contentId);
      if(entitlementResult.decision!=="granted")return res.status(403).json({error:"Demo entitlement is not granted.",entitlement:entitlementResult});
      const session=await demoPlayback.createSession("demo-user",contentId);
      if(session.status!=="demo_preview"||!["demo-free","demo-premium"].includes(session.mediaRef))
        return res.status(403).json({error:"No playable demo media is assigned to this content.",session});
      return res.status(201).json(session);
    } catch(error){next(error);}
  });
  app.post("/v1/demo/live/sessions",async(req,res,next)=>{
    if(!demoMode)return res.status(404).json({error:"Demo mode is disabled."});
    const liveClassId=req.body?.liveClassId;
    if(typeof liveClassId!=="string"||!liveClassId.trim())return res.status(400).json({error:"liveClassId is required."});
    try {
      const entitlementResult=await demoEntitlement.check("demo-user",liveClassId);
      if(entitlementResult.decision!=="granted")return res.status(403).json({error:"Demo live entitlement is not granted.",entitlement:entitlementResult});
      const session=await demoLiveJoin.createJoinSession("demo-user",liveClassId);
      if(session.status!=="joined"||session.roomType!=="local_demo")return res.status(403).json({error:"Demo room could not be joined.",session});
      return res.status(201).json(session);
    } catch(error){next(error);}
  });
  app.post("/v1/demo/live/sessions/:liveClassId/leave",async(req,res,next)=>{
    if(!demoMode)return res.status(404).json({error:"Demo mode is disabled."});
    try {
      const left=await demoLiveJoin.leaveSession("demo-user",req.params.liveClassId);
      return res.json({liveClassId:req.params.liveClassId,status:"left",sessionClosed:left,source:"demo_only"});
    } catch(error){next(error);}
  });
  app.get("/v1/catalogue",async(req,res,next)=>{
    const page=Number(req.query.page??1), pageSize=Number(req.query.pageSize??20);
    if(!Number.isInteger(page)||page<1||!Number.isInteger(pageSize)||pageSize<1||pageSize>100)
      return res.status(400).json({error:"page must be >= 1 and pageSize must be between 1 and 100."});
    try {
      const hierarchy=await catalogue.listCataloguePage(page,pageSize);
      const legacyRows=await dbAll(db,"SELECT id,title,subject,teacher,description,starts_at AS \"startsAt\",status,join_url AS \"joinUrl\",playback_url AS \"playbackUrl\",visibility,demo_access AS \"demoAccess\",chapter_id AS \"chapterId\",type FROM classes ORDER BY title,id");
      const legacyLive=legacyRows.filter(row=>row.type==="live").map(row=>({...row,joinUrl:row.visibility==="free"?row.joinUrl:null,playbackUrl:null,demoAccess:Boolean(row.demoAccess)}));
      const legacyRecorded=legacyRows.filter(row=>row.type==="recorded").map(row=>({...row,joinUrl:null,playbackUrl:null,demoAccess:Boolean(row.demoAccess)}));
      const subjects=await dbAll(db,"SELECT DISTINCT id,name FROM subjects ORDER BY name,id");
      const schedules=await dbAll(db,"SELECT s.id,s.class_id AS \"classId\",s.starts_at AS \"startsAt\",s.ends_at AS \"endsAt\" FROM schedules s JOIN classes c ON c.id=s.class_id ORDER BY s.starts_at,s.id");
      const liveClasses=await catalogue.listLiveClasses();
      return res.json({...hierarchy,liveClasses,legacyLive,legacyRecorded,subjects,schedules});
    } catch(e){next(e);}
  });
  app.get("/v1/batches",async(_req,res,next)=>{try{res.json(await catalogue.listBatches());}catch(e){next(e);}});
  app.get("/v1/courses",async(req,res,next)=>{try{res.json(await catalogue.listCourses(req.query.batchId));}catch(e){next(e);}});
  app.get("/v1/courses/:courseId",async(req,res,next)=>{try{const value=await catalogue.getCourse(req.params.courseId);return value?res.json(value):res.status(404).json({error:"Course not found."});}catch(e){next(e);}});
  app.get("/v1/courses/:courseId/chapters",async(req,res,next)=>{try{res.json(await catalogue.listChapters(req.params.courseId));}catch(e){next(e);}});
  app.get("/v1/chapters/:chapterId/lectures",async(req,res,next)=>{try{res.json(await catalogue.listLectures(req.params.chapterId,"recorded"));}catch(e){next(e);}});
  app.get("/v1/live-classes",async(_req,res,next)=>{try{res.json(await catalogue.listLiveClasses());}catch(e){next(e);}});
  app.get("/v1/entitlements/content/:contentId",requireAuth,async(req,res,next)=>{try{res.json(await entitlement.check(req.user.sub,req.params.contentId));}catch(e){next(e);}});
  app.post("/v1/playback/sessions/:sessionId/refresh",requireAuth,async(req,res,next)=>{
    if(typeof playback.refreshSession!=="function")return res.status(503).json({error:"Playback session refresh is not configured."});
    try {
      const session=await playback.refreshSession(req.user.sub,req.params.sessionId);
      if(!session||session.id!==req.params.sessionId||typeof session.contentId!=="string"||
        !session.contentId||session.status!=="created"||
        typeof session.playbackUrl!=="string"||!session.playbackUrl.startsWith("https://"))
        return res.status(503).json({error:"Authorized playback session refresh is unavailable."});
      const access=await entitlement.check(req.user.sub,session.contentId);
      if(access.contentId!==session.contentId||access.decision!=="granted")
        return res.status(403).json({error:"Content entitlement is no longer granted.",entitlement:access});
      return res.json(session);
    } catch(error){next(error);}
  });
  app.delete("/v1/playback/sessions/:sessionId",requireAuth,async(req,res,next)=>{
    if(typeof playback.closeSession!=="function")return res.status(503).json({error:"Playback session close is not configured."});
    try {
      const result=await playback.closeSession(req.user.sub,req.params.sessionId);
      if(!result||result.status!=="closed")return res.status(503).json({error:"Playback session could not be closed."});
      return res.json({sessionId:req.params.sessionId,status:"closed"});
    } catch(error){next(error);}
  });
  app.post("/v1/playback/sessions",requireAuth,async(req,res,next)=>{
    const contentId=req.body?.contentId;
    if(typeof contentId!=="string"||!contentId.trim())return res.status(400).json({error:"contentId is required."});
    try {
      const result=await entitlement.check(req.user.sub,contentId);
      if(result.decision!=="granted")return res.status(403).json({error:"Content entitlement is not granted.",entitlement:result});
      const session=await playback.createSession(req.user.sub,contentId);
      if(!session||session.status!=="created"||
        typeof session.playbackUrl!=="string"||!session.playbackUrl.startsWith("https://")) {
        return res.status(503).json({error:"Authorized playback is not configured."});
      }
      return res.status(201).json(session);
    } catch(e){next(e);}
  });

  const publicClasses=type=>async(_req,res,next)=>{
    try {
      const sql=`SELECT c.id,c.title,c.subject,c.teacher,c.description,c.starts_at AS "startsAt",c.status,
        CASE WHEN c.visibility='free' THEN c.join_url ELSE NULL END AS "joinUrl",
        CASE WHEN c.visibility='free' THEN c.playback_url ELSE NULL END AS "playbackUrl",
        c.visibility,c.demo_access AS "demoAccess",c.chapter_id AS "chapterId"
        FROM classes c WHERE c.type=? ORDER BY c.starts_at IS NULL,c.starts_at ASC`;
      const rows=await dbAll(db,sql,type);
      return res.json(rows.map(row=>({...row,demoAccess:Boolean(row.demoAccess)})));
    } catch(e){next(e);}
  };
  app.get("/v1/classes/live",publicClasses("live"));
  app.get("/v1/classes/recorded",publicClasses("recorded"));
  app.get("/v1/subjects",async(_req,res,next)=>{try{res.json(await dbAll(db,"SELECT DISTINCT id,name FROM subjects ORDER BY name"));}catch(e){next(e);}});
  app.get("/v1/subjects/:subjectId/chapters",async(req,res,next)=>{try{res.json(await dbAll(db,"SELECT id,subject_id AS \"subjectId\",name FROM chapters WHERE subject_id=? ORDER BY name",req.params.subjectId));}catch(e){next(e);}});
  app.get("/v1/schedule",async(_req,res,next)=>{try{res.json(await dbAll(db,"SELECT s.id,s.class_id AS \"classId\",s.starts_at AS \"startsAt\",s.ends_at AS \"endsAt\" FROM schedules s JOIN classes c ON c.id=s.class_id ORDER BY s.starts_at ASC"));}catch(e){next(e);}});
  app.use((err,_req,res,_next)=>{
    if(err.message==="Origin is not allowed by CORS.")return res.status(403).json({error:err.message});
    if(err instanceof ProviderNotConfiguredError)return res.status(503).json({error:err.message,code:err.code});
    if(err instanceof ProviderAuthorizationError)return res.status(403).json({error:err.message,code:err.code});
    if(err instanceof ProviderAuthenticationError)return res.status(502).json({error:err.message,code:err.code});
    if(err instanceof ProviderContractError)return res.status(502).json({error:"Provider response was invalid.",code:err.code});
    if(err instanceof ProviderError)return res.status(err.retryable?503:502).json({error:"Provider request failed.",code:err.code});
    return res.status(500).json({error:"Unexpected server error."});
  });
  return app;
}

if(process.argv[1] && import.meta.url===pathToFileURL(process.argv[1]).href) {
  const PORT=Number(process.env.PORT||8080),HOST=process.env.HOST||"0.0.0.0";
  const start=async()=>{
    assertDatabaseConfiguration(process.env);
    const db=process.env.DATABASE_URL?await createPostgresDatabase():createDatabase(path.resolve(process.env.DATABASE_FILE||"./data/app.sqlite"));
    const server=createApp({db}).listen(PORT,HOST,()=>console.log("Balidan RWA API listening on "+HOST+":"+PORT));
    const shutdown=async()=>{server.close();if(typeof db.close==="function")await db.close();process.exit(0);};
    process.once("SIGTERM",shutdown);
    process.once("SIGINT",shutdown);
  };
  start().catch(error=>{console.error("API startup failed:",error.message);process.exit(1);});
}
