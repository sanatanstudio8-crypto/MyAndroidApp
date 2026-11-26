import test from "node:test";
import assert from "node:assert/strict";
import request from "supertest";
import jwt from "jsonwebtoken";
import Database from "better-sqlite3";
import { migrateDatabase } from "../src/db/migrations.js";
import { createApp } from "../src/server.js";

const secret="test-secret-with-at-least-thirty-two-characters";
function fixture(overrides={}) {
  const db=new Database(":memory:"); db.pragma("foreign_keys=ON"); migrateDatabase(db);
  const app=createApp({db,jwtSecret:secret,providers:overrides});
  return {db,app};
}
test("public stable catalogue routes return empty arrays without fabricated content",async()=>{
  const {db,app}=fixture();
  for(const route of ["/v1/batches","/v1/courses","/v1/live-classes"]) {
    const response=await request(app).get(route).expect(200);
    assert.deepEqual(response.body,[]);
  }
  db.close();
});
test("legacy demo_access flag never exposes Premium live or recording URLs",async()=>{
  const {db,app}=fixture();
  db.prepare("INSERT INTO classes(id,title,subject,teacher,description,status,type,join_url,playback_url,visibility,demo_access,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)")
    .run("premium-demo-flag","Premium class","Subject","Teacher","", "scheduled","recorded","https://join.example.test","https://media.example.test/premium.m3u8","paid",1,new Date().toISOString());
  const response=await request(app).get("/v1/classes/recorded").expect(200);
  assert.equal(response.body[0].demoAccess,true);
  assert.equal(response.body[0].joinUrl,null);
  assert.equal(response.body[0].playbackUrl,null);
  db.close();
});
test("course, chapter and lecture routes use provider DTOs",async()=>{
  const catalogue={
    listBatches:async()=>[{id:"b1",title:"Batch"}],
    listCourses:async()=>[{id:"c1",title:"Course"}],
    getCourse:async()=>({id:"c1",title:"Course"}),
    listChapters:async()=>[{id:"ch1",courseId:"c1",title:"Chapter"}],
    listLectures:async()=>[{id:"l1",chapterId:"ch1",title:"Lecture",type:"recorded"}],
    listLiveClasses:async()=>[{id:"live1",title:"Live"}]
  };
  const {db,app}=fixture({catalogue});
  assert.equal((await request(app).get("/v1/batches").expect(200)).body[0].id,"b1");
  assert.equal((await request(app).get("/v1/courses?batchId=b1").expect(200)).body[0].id,"c1");
  assert.equal((await request(app).get("/v1/courses/c1").expect(200)).body.title,"Course");
  assert.equal((await request(app).get("/v1/courses/c1/chapters").expect(200)).body[0].id,"ch1");
  assert.equal((await request(app).get("/v1/chapters/ch1/lectures").expect(200)).body[0].type,"recorded");
  assert.equal((await request(app).get("/v1/live-classes").expect(200)).body[0].id,"live1");
  db.close();
});
test("entitlements require auth and unknown never grants playback",async()=>{
  const {db,app}=fixture();
  await request(app).get("/v1/entitlements/content/lecture-1").expect(401);
  const token=jwt.sign({sub:"u1"},secret,{issuer:"balidan-rwa-api"});
  const entitlement=await request(app).get("/v1/entitlements/content/lecture-1").set("Authorization",`Bearer ${token}`).expect(200);
  assert.equal(entitlement.body.decision,"unknown");
  await request(app).post("/v1/playback/sessions").set("Authorization",`Bearer ${token}`).send({contentId:"lecture-1"}).expect(403);
  db.close();
});
test("playback does not expose URLs when no authorized provider is configured",async()=>{
  const {db,app}=fixture({entitlement:{check:async(_u,contentId)=>({contentId,decision:"granted"})}});
  const token=jwt.sign({sub:"u1"},secret,{issuer:"balidan-rwa-api"});
  const response=await request(app).post("/v1/playback/sessions").set("Authorization",`Bearer ${token}`).send({contentId:"l1"}).expect(503);
  assert.equal(response.body.error,"Authorized playback is not configured.");
  assert.equal(response.body.playbackUrl,undefined);
  db.close();
});

test("isolated demo media and live rooms use server fixtures and cannot authorize real provider playback",async()=>{
  let playbackCalls=0;
  const db=new Database(":memory:"); db.pragma("foreign_keys=ON"); migrateDatabase(db);
  const providers={
    entitlement:{check:async(_userId,contentId)=>({contentId,decision:"unknown"})},
    playback:{createSession:async()=>{playbackCalls++;return {status:"created",playbackUrl:"https://media.example.test/video.m3u8"};}}
  };
  const demoApp=createApp({db,jwtSecret:secret,providers,demoMode:true});
  const catalogue=await request(demoApp).get("/v1/demo/catalogue").expect(200);
  assert.equal(catalogue.body.mode,"demo");
  assert.equal(catalogue.body.batches.length,2);
  assert.equal(catalogue.body.courses.length,2);
  assert.equal(catalogue.body.chapters.length,2);
  assert.equal(catalogue.body.lectures.find(x=>x.id==="demo-premium-lecture").visibility,"paid");
  assert.equal(catalogue.body.liveClasses.length,2);

  for (const [contentId,decision] of [
    ["demo-free-lecture","granted"],["demo-premium-lecture","granted"],
    ["demo-denied-lecture","denied"],["demo-expired-lecture","expired"],
    ["demo-not-known","unknown"]
  ]) {
    const response=await request(demoApp).get("/v1/demo/entitlements/content/"+contentId).expect(200);
    assert.equal(response.body.decision,decision);
    assert.equal(response.body.source,"demo_only");
  }
  const free=await request(demoApp).post("/v1/demo/playback/sessions").send({contentId:"demo-free-lecture"}).expect(201);
  assert.equal(free.body.status,"demo_preview");
  assert.equal(free.body.mediaRef,"demo-free");
  assert.equal(free.body.playbackUrl,undefined);
  const premium=await request(demoApp).post("/v1/demo/playback/sessions").send({contentId:"demo-premium-lecture"}).expect(201);
  assert.equal(premium.body.mediaRef,"demo-premium");
  assert.equal(premium.body.providerAuthorization,false);
  for (const contentId of ["demo-denied-lecture","demo-expired-lecture","demo-not-known"]) {
    await request(demoApp).post("/v1/demo/playback/sessions").send({contentId}).expect(403);
  }

  const freeRoom=await request(demoApp).post("/v1/demo/live/sessions").send({liveClassId:"demo-free-live-class"}).expect(201);
  assert.equal(freeRoom.body.status,"joined");
  assert.equal(freeRoom.body.roomType,"local_demo");
  assert.equal(freeRoom.body.joinUrl,undefined);
  const premiumRoom=await request(demoApp).post("/v1/demo/live/sessions").send({liveClassId:"demo-premium-live-class"}).expect(201);
  assert.equal(premiumRoom.body.status,"joined");
  for (const liveClassId of ["demo-denied-live-class","demo-expired-live-class","demo-unknown-live-class"]) {
    await request(demoApp).post("/v1/demo/live/sessions").send({liveClassId}).expect(403);
  }
  const leave=await request(demoApp).post("/v1/demo/live/sessions/demo-free-live-class/leave").expect(200);
  assert.equal(leave.body.status,"left");
  assert.equal(leave.body.sessionClosed,true);

  const token=jwt.sign({sub:"demo-user"},secret,{issuer:"balidan-rwa-api"});
  const real=await request(demoApp).post("/v1/playback/sessions")
    .set("Authorization",`Bearer ${token}`).send({contentId:"demo-premium-lecture"}).expect(403);
  assert.equal(real.body.entitlement.decision,"unknown");
  assert.equal(playbackCalls,0);
  const productionApp=createApp({db,jwtSecret:secret,providers,demoMode:false});
  const disabled=await request(productionApp).get("/v1/demo/catalogue").expect(404);
  assert.equal(disabled.body.error,"Demo mode is disabled.");
  await request(productionApp).post("/v1/demo/playback/sessions").send({contentId:"demo-premium-lecture"}).expect(404);
  db.close();
});

test("explicitly denied Premium entitlement never reaches the playback provider",async()=>{
  let playbackCalls=0;
  const {db,app}=fixture({
    entitlement:{check:async(_userId,contentId)=>({contentId,decision:"denied"})},
    playback:{createSession:async()=>{playbackCalls++;return {status:"created",playbackUrl:"https://media.example.test/should-not-play.m3u8"};}}
  });
  const token=jwt.sign({sub:"u-denied"},secret,{issuer:"balidan-rwa-api"});
  const response=await request(app).post("/v1/playback/sessions")
    .set("Authorization",`Bearer ${token}`).send({contentId:"premium-lecture"}).expect(403);
  assert.equal(response.body.entitlement.decision,"denied");
  assert.equal(playbackCalls,0);
  db.close();
});
test("a non-created playback session is never returned as playable",async()=>{
  const {db,app}=fixture({
    entitlement:{check:async(_userId,contentId)=>({contentId,decision:"granted"})},
    playback:{createSession:async(_userId,contentId)=>({contentId,status:"unavailable",reason:"provider unavailable"})}
  });
  const token=jwt.sign({sub:"u-unavailable"},secret,{issuer:"balidan-rwa-api"});
  const response=await request(app).post("/v1/playback/sessions")
    .set("Authorization",`Bearer ${token}`).send({contentId:"lecture-unavailable"}).expect(503);
  assert.equal(response.body.error,"Authorized playback is not configured.");
  assert.equal(response.body.playbackUrl,undefined);
  db.close();
});

test("a created session without an HTTPS playback URL cannot be started",async()=>{
  const {db,app}=fixture({
    entitlement:{check:async(_userId,contentId)=>({contentId,decision:"granted"})},
    playback:{createSession:async(_userId,contentId)=>({contentId,status:"created",playbackUrl:"http://insecure.example.test/video.m3u8"})}
  });
  const token=jwt.sign({sub:"u-insecure"},secret,{issuer:"balidan-rwa-api"});
  const response=await request(app).post("/v1/playback/sessions")
    .set("Authorization",`Bearer ${token}`).send({contentId:"lecture-insecure"}).expect(503);
  assert.equal(response.body.playbackUrl,undefined);
  db.close();
});

test("paginated catalogue snapshot preserves hierarchy, stable ordering and visibility",async()=>{
  const {db,app}=fixture();
  db.prepare("INSERT INTO subjects(id,name) VALUES(?,?)").run("batch-2","Batch B");
  db.prepare("INSERT INTO subjects(id,name) VALUES(?,?)").run("batch-1","Batch A");
  db.prepare("INSERT INTO courses(id,batch_id,title,visibility,created_at) VALUES(?,?,?,?,?)").run("course-1","batch-1","Course A","free",new Date().toISOString());
  db.prepare("INSERT INTO courses(id,batch_id,title,visibility,created_at) VALUES(?,?,?,?,?)").run("course-2","batch-2","Course B","paid",new Date().toISOString());
  db.prepare("INSERT INTO course_chapters(id,course_id,title,position) VALUES(?,?,?,?)").run("chapter-1","course-1","Chapter 1",1);
  db.prepare("INSERT INTO lectures(id,chapter_id,title,lecture_type,visibility,created_at) VALUES(?,?,?,?,?,?)").run("lecture-1","chapter-1","Lecture 1","recorded","free",new Date().toISOString());
  const first=await request(app).get("/v1/catalogue?page=1&pageSize=1").expect(200);
  assert.equal(first.body.items.length,1);
  assert.equal(first.body.items[0].id,"batch-1");
  assert.equal(first.body.items[0].courses[0].id,"course-1");
  assert.equal(first.body.items[0].courses[0].visibility,"free");
  assert.equal(first.body.items[0].courses[0].chapters[0].id,"chapter-1");
  assert.equal(first.body.items[0].courses[0].chapters[0].lectures[0].id,"lecture-1");
  assert.equal(first.body.hasNextPage,true);
  const second=await request(app).get("/v1/catalogue?page=2&pageSize=1").expect(200);
  assert.equal(second.body.items[0].id,"batch-2");
  assert.equal(second.body.items[0].courses[0].visibility,"paid");
  assert.equal(second.body.hasNextPage,false);
  await request(app).get("/v1/catalogue?page=0&pageSize=1").expect(400);
  await request(app).get("/v1/catalogue?page=1&pageSize=101").expect(400);
  db.close();
});


test("controlled playback provider covers Free/Premium decisions and create-refresh-close lifecycle",async()=>{
  const sessions=new Map();
  const calls=[];
  const decisions=new Map([
    ["free-lecture","granted"],["premium-lecture","granted"],["revoked-after-create","granted"],
    ["denied-lecture","denied"],["unknown-lecture","unknown"],
    ["expired-lecture","expired"]
  ]);
  const providers={
    entitlement:{check:async(_userId,contentId)=>({
      contentId,decision:decisions.get(contentId)||"unknown"
    })},
    playback:{
      createSession:async(userId,contentId)=>{
        calls.push(["create",userId,contentId]);
        const session={id:"test-session-"+contentId,contentId,status:"created",
          protocol:"progressive",playbackUrl:"https://authorized.test/"+contentId+".mp4",
          expiresAt:new Date(Date.now()+60000).toISOString()};
        sessions.set(session.id,{...session,userId});
        return session;
      },
      refreshSession:async(userId,sessionId)=>{
        calls.push(["refresh",userId,sessionId]);
        const current=sessions.get(sessionId);
        if(!current||current.userId!==userId)return null;
        const refreshed={...current,expiresAt:new Date(Date.now()+120000).toISOString()};
        sessions.set(sessionId,refreshed);
        return refreshed;
      },
      closeSession:async(userId,sessionId)=>{
        calls.push(["close",userId,sessionId]);
        const current=sessions.get(sessionId);
        if(!current||current.userId!==userId)return {status:"not_found"};
        sessions.delete(sessionId);
        return {status:"closed"};
      }
    }
  };
  const {db,app}=fixture(providers);
  const token=jwt.sign({sub:"phase2-test-user"},secret,{issuer:"balidan-rwa-api"});
  const auth={Authorization:`Bearer ${token}`};

  for(const contentId of ["free-lecture","premium-lecture"]) {
    const created=await request(app).post("/v1/playback/sessions")
      .set(auth).send({contentId}).expect(201);
    assert.equal(created.body.contentId,contentId);
    assert.equal(created.body.status,"created");
    assert.match(created.body.playbackUrl,/^https:\/\//);
    const refreshed=await request(app).post("/v1/playback/sessions/"+created.body.id+"/refresh")
      .set(auth).expect(200);
    assert.equal(refreshed.body.id,created.body.id);
    assert.equal(refreshed.body.contentId,contentId);
    assert.equal(refreshed.body.status,"created");
    const closed=await request(app).delete("/v1/playback/sessions/"+created.body.id)
      .set(auth).expect(200);
    assert.deepEqual(closed.body,{sessionId:created.body.id,status:"closed"});
    assert.equal(sessions.has(created.body.id),false);
  }

  const revocable=await request(app).post("/v1/playback/sessions")
    .set(auth).send({contentId:"revoked-after-create"}).expect(201);
  decisions.set("revoked-after-create","denied");
  const refreshDenied=await request(app).post("/v1/playback/sessions/"+revocable.body.id+"/refresh")
    .set(auth).expect(403);
  assert.equal(refreshDenied.body.entitlement.decision,"denied");

  for(const contentId of ["denied-lecture","unknown-lecture","expired-lecture"]) {
    const denied=await request(app).post("/v1/playback/sessions")
      .set(auth).send({contentId}).expect(403);
    assert.equal(denied.body.entitlement.decision,
      contentId==="denied-lecture"?"denied":contentId==="unknown-lecture"?"unknown":"expired");
  }
  assert.equal(calls.filter(call=>call[0]==="create").length,3);
  assert.equal(calls.filter(call=>call[0]==="refresh").length,3);
  assert.equal(calls.filter(call=>call[0]==="close").length,2);
  db.close();
});
