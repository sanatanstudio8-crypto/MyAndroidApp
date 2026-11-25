import test from "node:test";
import assert from "node:assert/strict";
import crypto from "node:crypto";
import { createPostgresDatabase } from "../src/server.js";
import { PostgresCatalogueProvider, PostgresEntitlementProvider } from "../src/providers/postgres-providers.js";

test("PostgreSQL RWA boundary keeps catalogue and entitlement persistence normalized", { skip: !process.env.DATABASE_URL }, async () => {
  const db = await createPostgresDatabase(process.env.DATABASE_URL);
  const suffix = crypto.randomUUID();
  const userId = `rwa-test-user-${suffix}`;
  const batchId = `rwa-test-batch-${suffix}`;
  const courseId = `rwa-test-course-${suffix}`;
  const chapterId = `rwa-test-chapter-${suffix}`;
  const lectureId = `rwa-test-lecture-${suffix}`;
  try {
    await db.run("INSERT INTO users(id,name,email,password_hash,created_at) VALUES(?,?,?,?,?)", userId, "RWA Boundary Test", `${suffix}@example.test`, "not-used", new Date().toISOString());
    await db.run("INSERT INTO subjects(id,name) VALUES(?,?)", batchId, `RWA Test ${suffix}`);
    await db.run("INSERT INTO courses(id,batch_id,title,visibility,created_at) VALUES(?,?,?,?,?)", courseId, batchId, "Normalized Course", "free", new Date().toISOString());
    await db.run("INSERT INTO course_chapters(id,course_id,title,position) VALUES(?,?,?,?)", chapterId, courseId, "Normalized Chapter", 1);
    await db.run("INSERT INTO lectures(id,chapter_id,title,lecture_type,visibility,created_at) VALUES(?,?,?,?,?,?)", lectureId, chapterId, "Normalized Lecture", "recorded", "free", new Date().toISOString());

    const catalogue = new PostgresCatalogueProvider(db);
    const entitlements = new PostgresEntitlementProvider(db);
    assert.equal((await catalogue.listBatches()).some(x => x.id === batchId), true);
    assert.equal((await catalogue.listCourses(batchId))[0].id, courseId);
    assert.equal((await catalogue.getCourse(courseId)).id, courseId);
    assert.equal((await catalogue.listChapters(courseId))[0].id, chapterId);
    assert.equal((await catalogue.listLectures(chapterId, "recorded"))[0].id, lectureId);
    assert.equal((await entitlements.check(userId, lectureId)).decision, "unknown");

    await db.run("INSERT INTO entitlement_checks(id,user_id,content_id,decision,checked_at) VALUES(?,?,?,?,?)", `rwa-test-ent-${suffix}`, userId, lectureId, "granted", new Date(Date.now() - 120000).toISOString());
    assert.equal((await entitlements.check(userId, lectureId)).decision, "granted");
    await db.run("INSERT INTO entitlement_checks(id,user_id,content_id,decision,checked_at,expires_at) VALUES(?,?,?,?,?,?)", `rwa-test-expired-${suffix}`, userId, lectureId, "granted", new Date().toISOString(), new Date(Date.now() - 60000).toISOString());
    assert.equal((await entitlements.check(userId, lectureId)).decision, "unknown");
  } finally {
    await db.run("DELETE FROM users WHERE id=?", userId);
    await db.run("DELETE FROM courses WHERE id=?", courseId);
    await db.run("DELETE FROM subjects WHERE id=?", batchId);
    await db.close();
  }
});
