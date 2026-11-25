import test from "node:test";
import assert from "node:assert/strict";
import request from "supertest";
import { createPostgresDatabase } from "../src/server.js";
import { migratePostgres } from "../src/db/postgres-migrations.js";
import { createApp } from "../src/server.js";

const secret = "postgres-integration-test-secret-at-least-32-characters";

test("PostgreSQL migrations, constraints, providers and API integration", { skip: !process.env.DATABASE_URL }, async () => {
  const db = await createPostgresDatabase(process.env.DATABASE_URL);
  try {
    const concurrentMigrationResults = await Promise.all([migratePostgres(db), migratePostgres(db)]);
    assert.deepEqual(concurrentMigrationResults, [2, 2]);
    assert.equal(await migratePostgres(db), 2);

    await db.exec("TRUNCATE playback_sessions, entitlement_checks, provider_content_metadata, live_classes, lectures, course_chapters, courses, provider_sessions, provider_accounts, providers, schedules, class_entitlements, classes, chapters, subjects, users CASCADE");

    const app = createApp({ db, jwtSecret: secret });
    const signup = await request(app).post("/v1/auth/signup").send({
      name: "Integration User",
      email: "Integration.User@example.com",
      password: "test-password-long-enough"
    }).expect(201);
    assert.equal(signup.body.tokenType, "Bearer");
    await request(app).post("/v1/auth/signup").send({
      name: "Duplicate User",
      email: "integration.user@EXAMPLE.com",
      password: "another-test-password"
    }).expect(409);

    const login = await request(app).post("/v1/auth/login").send({
      email: "INTEGRATION.USER@example.com",
      password: "test-password-long-enough"
    }).expect(200);
    const token = login.body.accessToken;
    assert.ok(token);
    const integrationUser = await db.get("SELECT id FROM users WHERE LOWER(email)=LOWER(?)", "integration.user@example.com");

    await db.run(
      "INSERT INTO entitlement_checks(id,user_id,content_id,decision,checked_at) VALUES(?,?,?,?,?)",
      "pg-denied-entitlement", integrationUser.id, "pg-denied-lecture", "denied", new Date().toISOString()
    );
    await request(app).post("/v1/playback/sessions")
      .set("Authorization", "Bearer " + token)
      .send({ contentId: "pg-denied-lecture" }).expect(403);

    await db.run(
      "INSERT INTO entitlement_checks(id,user_id,content_id,decision,checked_at) VALUES(?,?,?,?,?)",
      "pg-granted-entitlement", integrationUser.id, "pg-granted-unavailable", "granted", new Date().toISOString()
    );
    const unavailablePlayback = await request(app).post("/v1/playback/sessions")
      .set("Authorization", "Bearer " + token)
      .send({ contentId: "pg-granted-unavailable" }).expect(503);
    assert.equal(unavailablePlayback.body.playbackUrl, undefined);

    await db.run("INSERT INTO courses(id,title,visibility,created_at) VALUES(?,?,?,?)", "pg-course-1", "PostgreSQL Course", "free", new Date().toISOString());
    await db.run("INSERT INTO course_chapters(id,course_id,title,position) VALUES(?,?,?,?)", "pg-chapter-1", "pg-course-1", "Chapter One", 1);
    await db.run("INSERT INTO lectures(id,chapter_id,title,lecture_type,visibility,created_at) VALUES(?,?,?,?,?,?)", "pg-lecture-1", "pg-chapter-1", "Lecture One", "recorded", "free", new Date().toISOString());

    const courses = await request(app).get("/v1/courses").expect(200);
    assert.equal(courses.body[0].id, "pg-course-1");
    const chapters = await request(app).get("/v1/courses/pg-course-1/chapters").expect(200);
    assert.equal(chapters.body[0].id, "pg-chapter-1");
    const lectures = await request(app).get("/v1/chapters/pg-chapter-1/lectures").expect(200);
    assert.equal(lectures.body[0].id, "pg-lecture-1");

    const entitlement = await request(app).get("/v1/entitlements/content/pg-lecture-1")
      .set("Authorization", "Bearer " + token).expect(200);
    assert.equal(entitlement.body.decision, "unknown");
    await request(app).post("/v1/playback/sessions")
      .set("Authorization", "Bearer " + token)
      .send({ contentId: "pg-lecture-1" }).expect(403);

    const demoApp = createApp({ db, jwtSecret: secret, demoMode: true });
    const demoEntitlement = await request(demoApp)
      .get("/v1/demo/entitlements/content/demo-premium-lecture").expect(200);
    assert.equal(demoEntitlement.body.decision, "demo_granted");
    assert.equal(demoEntitlement.body.providerAuthorization, false);
    const demoPreview = await request(demoApp).post("/v1/demo/playback/sessions")
      .send({ contentId: "demo-premium-lecture" }).expect(200);
    assert.equal(demoPreview.body.status, "demo_preview");
    assert.equal(demoPreview.body.playbackUrl, null);
    await request(demoApp).post("/v1/playback/sessions")
      .set("Authorization", "Bearer " + token)
      .send({ contentId: "demo-premium-lecture" }).expect(403);

    await assert.rejects(
      db.run("INSERT INTO course_chapters(id,course_id,title,position) VALUES(?,?,?,?)", "bad-chapter", "missing-course", "Invalid", 0),
      error => error.code === "23503"
    );

    const legacy = await db.all("SELECT id FROM users WHERE LOWER(email)=LOWER(?)", "integration.user@example.com");
    assert.equal(legacy.length, 1);
  } finally {
    await db.close();
  }
});
