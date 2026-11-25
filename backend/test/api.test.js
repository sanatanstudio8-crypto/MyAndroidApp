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
