import test from "node:test";
import assert from "node:assert/strict";
import { assertDatabaseConfiguration } from "../src/server.js";

test("staging rejects a missing PostgreSQL connection string", () => {
  assert.throws(
    () => assertDatabaseConfiguration({ REQUIRE_POSTGRES: "true" }),
    /DATABASE_URL is required/
  );
});

test("staging rejects non-PostgreSQL database URLs", () => {
  assert.throws(
    () => assertDatabaseConfiguration({ REQUIRE_POSTGRES: "true", DATABASE_URL: "sqlite://local.db" }),
    /PostgreSQL DATABASE_URL/
  );
});

test("staging accepts PostgreSQL connection URL schemes", () => {
  assert.doesNotThrow(() => assertDatabaseConfiguration({
    REQUIRE_POSTGRES: "true",
    DATABASE_URL: "postgresql://user:secret@db.internal:5432/balidan"
  }));
  assert.doesNotThrow(() => assertDatabaseConfiguration({
    REQUIRE_POSTGRES: "true",
    DATABASE_URL: "postgres://user:secret@db.internal:5432/balidan"
  }));
});

test("local development retains the existing SQLite fallback unless PostgreSQL is required", () => {
  assert.doesNotThrow(() => assertDatabaseConfiguration({ REQUIRE_POSTGRES: "false" }));
  assert.doesNotThrow(() => assertDatabaseConfiguration({}));
});
