import test from "node:test";
import assert from "node:assert/strict";
import Database from "better-sqlite3";
import {
  MockRwaIdentityProvider, MockRwaCatalogueProvider, MockRwaEntitlementProvider,
  MockRwaPlaybackProvider, MockLiveJoinProvider
} from "../src/providers/mocks/mock-rwa-providers.js";
import { createDisabledRwaProviders } from "../src/providers/rwa/disabled-rwa-providers.js";
import { batchDto, courseDto, entitlementDto, playbackSessionDto } from "../src/providers/rwa/dtos.js";
import { ProviderAuthorizationError, ProviderAuthenticationError, ProviderContractError, ProviderNotConfiguredError } from "../src/providers/errors.js";
import { createApp } from "../src/server.js";
import { migrateDatabase } from "../src/db/migrations.js";
import { SqliteCatalogueProvider, SqliteEntitlementProvider } from "../src/providers/sqlite-providers.js";

test("normalized DTOs validate required identity fields and fail closed on unknown decisions", () => {
  assert.equal(batchDto({ id: "b1", title: "Batch" }).name, "Batch");
  assert.equal(courseDto({ id: "c1", title: "Course", visibility: "unexpected" }).visibility, "paid");
  assert.equal(entitlementDto({ contentId: "l1", decision: "demo_granted" }).decision, "unknown");
  assert.throws(() => playbackSessionDto({ contentId: "l1", status: "created", playbackUrl: "http://insecure.test/a" }), ProviderContractError);
  assert.throws(() => batchDto({ id: "b1" }), ProviderContractError);
});

test("mock identity supports authenticate, refresh, validate and revoke without provider tokens", async () => {
  const identity = new MockRwaIdentityProvider();
  await assert.rejects(identity.authenticate({ email: "wrong", password: "wrong" }), ProviderAuthenticationError);
  const session = await identity.authenticate({ email: "student@example.test", password: "mock-password" });
  assert.equal(session.status, "authenticated");
  assert.match(session.tokenRef, /^mock-session:/);
  assert.equal("accessToken" in session, false);
  assert.equal(await identity.validateSession(session.userId), true);
  assert.equal((await identity.refreshSession(session.userId)).status, "authenticated");
  assert.equal(await identity.revokeSession(session.userId), true);
  assert.equal(await identity.validateSession(session.userId), false);
});

test("mock catalogue returns normalized DTOs and filters by parent/type", async () => {
  const catalogue = new MockRwaCatalogueProvider({
    batches: [{ id: "b1", title: "Batch One" }],
    courses: [{ id: "c1", batchId: "b1", title: "Course One" }, { id: "c2", batchId: "b2", title: "Course Two" }],
    chapters: [{ id: "ch1", courseId: "c1", title: "Chapter One" }],
    lectures: [{ id: "r1", chapterId: "ch1", title: "Recorded", type: "recorded" }, { id: "l1", chapterId: "ch1", title: "Live", type: "live" }],
    liveClasses: [{ id: "live1", title: "Live Class" }]
  });
  assert.equal((await catalogue.listBatches())[0].id, "b1");
  assert.deepEqual((await catalogue.listCourses("b1")).map(x => x.id), ["c1"]);
  assert.equal((await catalogue.getCourse("missing")), null);
  assert.equal((await catalogue.listChapters("c1"))[0].courseId, "c1");
  assert.deepEqual((await catalogue.listLectures("ch1", "recorded")).map(x => x.id), ["r1"]);
  assert.equal((await catalogue.listLiveClasses())[0].id, "live1");
});

test("entitlements default to unknown and only explicit grants are granted", async () => {
  const entitlements = new MockRwaEntitlementProvider({
    accountAccess: { u1: "granted" },
    courseAccess: { "u1:c1": "denied" },
    lectureAccess: { "u1:l1": "granted" },
    subscriptions: { u1: { status: "active", source: "mock" } }
  });
  assert.equal((await entitlements.checkAccountAccess("missing")).decision, "unknown");
  assert.equal((await entitlements.checkCourseAccess("u1", "c1")).decision, "denied");
  assert.equal((await entitlements.checkLectureAccess("u1", "l1")).decision, "granted");
  assert.equal((await entitlements.getSubscriptionStatus("u1")).status, "active");
});

test("mock playback requires a grant, isolates users, and never returns a playable URL", async () => {
  const entitlements = new MockRwaEntitlementProvider({ lectureAccess: { "u1:l1": "granted" } });
  const playback = new MockRwaPlaybackProvider({ entitlementProvider: entitlements });
  await assert.rejects(playback.createSession("u2", "l1"), ProviderAuthorizationError);
  await assert.rejects(playback.createSession("u1", "unknown"), ProviderAuthorizationError);
  const session = await playback.createSession("u1", "l1");
  assert.equal(session.status, "mock_created");
  assert.equal(session.playbackUrl, null);
  assert.equal(session.providerAuthorization, false);
  assert.equal((await playback.refreshSession("u1", "l1")).id, session.id);
  assert.equal(await playback.refreshSession("u2", "l1").catch(() => null), null);
  assert.equal(await playback.closeSession("u1", "l1"), true);
});

test("live joining is a separate mock contract and never returns a join URL", async () => {
  const entitlements = new MockRwaEntitlementProvider({ lectureAccess: { "u1:live1": "granted" } });
  const liveJoin = new MockLiveJoinProvider({ entitlementProvider: entitlements });
  await assert.rejects(liveJoin.createJoinSession("u2", "live1"), ProviderAuthorizationError);
  const session = await liveJoin.createJoinSession("u1", "live1");
  assert.equal(session.status, "mock_created");
  assert.equal(session.joinUrl, null);
  assert.equal(session.providerAuthorization, false);
  assert.equal((await liveJoin.refreshJoinSession("u1", "live1")).id, session.id);
  assert.equal(await liveJoin.leaveSession("u1", "live1"), true);
});

test("disabled real providers never make network requests and always report not configured", async () => {
  const providers = createDisabledRwaProviders();
  assert.equal(providers.enabled, false);
  for (const action of [
    () => providers.identity.authenticate({}),
    () => providers.catalogue.listBatches(),
    () => providers.entitlement.checkLectureAccess("u1", "l1"),
    () => providers.playback.createSession("u1", "l1"),
    () => providers.liveJoin.createJoinSession("u1", "live1")
  ]) await assert.rejects(action, ProviderNotConfiguredError);
});

test("SQLite catalogue and entitlement providers retain existing Balidan persistence behavior", async () => {
  const db = new Database(":memory:");
  db.pragma("foreign_keys=ON");
  migrateDatabase(db);
  db.prepare("INSERT INTO subjects(id,name) VALUES(?,?)").run("b1", "Batch One");
  db.prepare("INSERT INTO courses(id,batch_id,title,visibility,created_at) VALUES(?,?,?,?,?)").run("c1", "b1", "Course One", "free", new Date().toISOString());
  db.prepare("INSERT INTO course_chapters(id,course_id,title,position) VALUES(?,?,?,?)").run("ch1", "c1", "Chapter One", 1);
  db.prepare("INSERT INTO lectures(id,chapter_id,title,lecture_type,visibility,created_at) VALUES(?,?,?,?,?,?)").run("l1", "ch1", "Lecture One", "recorded", "free", new Date().toISOString());
  db.prepare("INSERT INTO users(id,name,email,password_hash,created_at) VALUES(?,?,?,?,?)").run("u1", "User", "u1@example.test", "not-used", new Date().toISOString());
  const catalogue = new SqliteCatalogueProvider(db);
  const entitlements = new SqliteEntitlementProvider(db);
  assert.equal(catalogue.listBatches()[0].id, "b1");
  assert.equal(catalogue.listCourses("b1")[0].id, "c1");
  assert.equal(catalogue.getCourse("c1").id, "c1");
  assert.equal(catalogue.listChapters("c1")[0].id, "ch1");
  assert.equal(catalogue.listLectures("ch1", "recorded")[0].id, "l1");
  assert.equal((await entitlements.check("u1", "l1")).decision, "unknown");
  db.prepare("INSERT INTO entitlement_checks(id,user_id,content_id,decision,checked_at) VALUES(?,?,?,?,?)").run("e1", "u1", "l1", "granted", new Date().toISOString());
  assert.equal((await entitlements.check("u1", "l1")).decision, "granted");
  db.close();
});

test("typed provider configuration errors map to a safe 503 response", async () => {
  const db = new Database(":memory:");
  db.pragma("foreign_keys=ON");
  migrateDatabase(db);
  const app = createApp({
    db,
    jwtSecret: "provider-error-test-secret-at-least-thirty-two-characters",
    providers: { catalogue: { listBatches: async () => { throw new ProviderNotConfiguredError("original-rwa"); } } }
  });
  const response = await (await import("supertest")).default(app).get("/v1/batches").expect(503);
  assert.equal(response.body.code, "PROVIDER_NOT_CONFIGURED");
  assert.equal(response.body.error, "Provider integration is not configured.");
  db.close();
});
