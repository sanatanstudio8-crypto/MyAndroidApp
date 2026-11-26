import test from "node:test";
import assert from "node:assert/strict";
import Database from "better-sqlite3";
import { createApp } from "../src/server.js";
import { migrateDatabase } from "../src/db/migrations.js";

const jwtSecret = "server-injection-test-secret-at-least-32-chars";

function makeDb() {
  const db = new Database(":memory:");
  db.pragma("foreign_keys = ON");
  migrateDatabase(db);
  return db;
}

test("Original RWA provider bundle is disabled by default", () => {
  const db = makeDb();
  const app = createApp({ db, jwtSecret });
  assert.equal(app.locals.originalRwaProviders.enabled, false);
  assert.equal(app.locals.originalRwaProviders.identity.constructor.name, "DisabledRwaIdentityProvider");
  assert.equal(app.locals.originalRwaProviders.catalogue.constructor.name, "DisabledRwaCatalogueProvider");
  assert.equal(app.locals.originalRwaProviders.entitlement.constructor.name, "DisabledRwaEntitlementProvider");
  assert.equal(app.locals.originalRwaProviders.playback.constructor.name, "DisabledRwaPlaybackProvider");
  assert.equal(app.locals.originalRwaProviders.liveJoin.constructor.name, "DisabledLiveJoinProvider");
  db.close();
});

test("incomplete explicitly enabled provider injection falls back to disabled adapters", () => {
  const db = makeDb();
  const app = createApp({
    db,
    jwtSecret,
    providers: { originalRwa: { enabled: true, configurationStatus: { ready: false } } }
  });
  assert.equal(app.locals.originalRwaProviders.enabled, false);
  assert.equal(app.locals.originalRwaProviders.catalogue.constructor.name, "DisabledRwaCatalogueProvider");
  db.close();
});
