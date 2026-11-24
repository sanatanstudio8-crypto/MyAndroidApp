import "dotenv/config";
import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import express from "express";
import cors from "cors";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import jwt from "jsonwebtoken";
import Database from "better-sqlite3";

const PORT=Number(process.env.PORT||8080), HOST=process.env.HOST||"127.0.0.1";
const JWT_SECRET=process.env.JWT_SECRET||"";
if(JWT_SECRET.length<32) throw new Error("JWT_SECRET must be configured with at least 32 characters.");
const databaseFile=path.resolve(process.env.DATABASE_FILE||"./data/app.sqlite");
fs.mkdirSync(path.dirname(databaseFile),{recursive:true});
const db=new Database(databaseFile); db.pragma("journal_mode = WAL"); db.pragma("foreign_keys = ON");
db.exec(`
CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY,name TEXT NOT NULL,email TEXT NOT NULL UNIQUE COLLATE NOCASE,password_hash TEXT NOT NULL,created_at TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS subjects (id TEXT PRIMARY KEY,name TEXT NOT NULL UNIQUE);
CREATE TABLE IF NOT EXISTS chapters (id TEXT PRIMARY KEY,subject_id TEXT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,name TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS classes (id TEXT PRIMARY KEY,title TEXT NOT NULL,subject TEXT NOT NULL,teacher TEXT NOT NULL,description TEXT,starts_at TEXT,status TEXT NOT NULL,type TEXT NOT NULL CHECK(type IN ('live','recorded')),join_url TEXT,playback_url TEXT,visibility TEXT NOT NULL DEFAULT 'paid' CHECK(visibility IN ('free','paid')),provider TEXT NOT NULL DEFAULT 'first_party',created_at TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS class_entitlements (user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,class_id TEXT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,PRIMARY KEY(user_id,class_id));
CREATE TABLE IF NOT EXISTS schedules (id TEXT PRIMARY KEY,class_id TEXT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,starts_at TEXT NOT NULL,ends_at TEXT);
`);
const app=express(); app.disable("x-powered-by"); app.use(helmet()); app.use(express.json({limit:"32kb"}));
const origins=(process.env.CORS_ORIGINS||"").split(",").map(v=>v.trim()).filter(Boolean);
app.use(cors({origin(origin,cb){if(!origin||origins.includes(origin)) return cb(null,true); return cb(new Error("Origin is not allowed by CORS."));}}));
const authLimiter=rateLimit({windowMs:15*60*1000,limit:10,standardHeaders:"draft-7",legacyHeaders:false});
function passwordDigest(password,salt=crypto.randomBytes(16).toString("hex")){return {salt,digest:crypto.scryptSync(password,salt,64).toString("hex")};}
function verifyPassword(password,stored){const [salt,digest]=stored.split(":");if(!salt||!digest)return false;const a=Buffer.from(passwordDigest(password,salt).digest,"hex"),b=Buffer.from(digest,"hex");return a.length===b.length&&crypto.timingSafeEqual(a,b);}
function issueToken(user){return jwt.sign({sub:user.id,email:user.email},JWT_SECRET,{expiresIn:"30m",issuer:"myandroidapp-api"});}
function validEmail(value){return typeof value==="string"&&/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);}
function authResponse(res,user){return res.json({accessToken:issueToken(user),tokenType:"Bearer"});}
app.get("/health",(_req,res)=>res.json({status:"ok"}));
app.post("/v1/auth/signup",authLimiter,(req,res)=>{
 const {name,email,password}=req.body||{};
 if(typeof name!=="string"||name.trim().length<2||!validEmail(email)||typeof password!=="string"||password.length<10)return res.status(400).json({error:"Provide a name, valid email and password of at least 10 characters."});
 const id=crypto.randomUUID(),{salt,digest}=passwordDigest(password);
 try{db.prepare("INSERT INTO users(id,name,email,password_hash,created_at) VALUES(?,?,?,?,?)").run(id,name.trim(),email.trim().toLowerCase(),salt+":"+digest,new Date().toISOString());return authResponse(res,{id,email:email.trim().toLowerCase()});}
 catch(e){if(String(e.code).includes("SQLITE_CONSTRAINT"))return res.status(409).json({error:"An account with this email already exists."});return res.status(500).json({error:"Account could not be created."});}
});
app.post("/v1/auth/login",authLimiter,(req,res)=>{
 const {email,password}=req.body||{};if(!validEmail(email)||typeof password!=="string")return res.status(400).json({error:"Email and password are required."});
 const user=db.prepare("SELECT id,email,password_hash FROM users WHERE email=? COLLATE NOCASE").get(email.trim());
 if(!user||!verifyPassword(password,user.password_hash))return res.status(401).json({error:"Email or password is incorrect."});return authResponse(res,user);
});
// Public catalogue returns only explicitly free classes. Paid classes and entitlements remain protected.
const publicClasses=type=>(_req,res)=>res.json(db.prepare(`SELECT c.id,c.title,c.subject,c.teacher,c.description,c.starts_at AS startsAt,c.status,c.join_url AS joinUrl,c.playback_url AS playbackUrl FROM classes c WHERE c.type=? AND c.visibility='free' ORDER BY c.starts_at IS NULL,c.starts_at ASC`).all(type));
app.get("/v1/classes/live",publicClasses("live"));
app.get("/v1/classes/recorded",publicClasses("recorded"));
app.get("/v1/subjects",(_req,res)=>res.json(db.prepare(`SELECT DISTINCT s.id,s.name FROM subjects s WHERE EXISTS(SELECT 1 FROM classes c WHERE c.visibility='free' AND c.subject=s.name) ORDER BY s.name`).all()));
app.get("/v1/subjects/:subjectId/chapters",(req,res)=>res.json(db.prepare(`SELECT ch.id,ch.subject_id AS subjectId,ch.name FROM chapters ch JOIN subjects s ON s.id=ch.subject_id WHERE ch.subject_id=? AND EXISTS(SELECT 1 FROM classes c WHERE c.visibility='free' AND c.subject=s.name) ORDER BY ch.name`).all(req.params.subjectId)));
app.get("/v1/schedule",(_req,res)=>res.json(db.prepare(`SELECT s.id,s.class_id AS classId,s.starts_at AS startsAt,s.ends_at AS endsAt FROM schedules s JOIN classes c ON c.id=s.class_id WHERE c.visibility='free' ORDER BY s.starts_at ASC`).all()));
app.use((err,_req,res,_next)=>{if(err.message==="Origin is not allowed by CORS.")return res.status(403).json({error:err.message});return res.status(500).json({error:"Unexpected server error."});});
app.listen(PORT,HOST,()=>console.log(`MyAndroidApp API listening on ${HOST}:${PORT}`));
