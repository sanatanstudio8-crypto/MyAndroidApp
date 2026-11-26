import test from "node:test";
import assert from "node:assert/strict";
import Database from "better-sqlite3";
import { migrateDatabase } from "../src/db/migrations.js";

test("migrations create schema and are idempotent",()=>{
  const db=new Database(":memory:");
  assert.equal(migrateDatabase(db),2);
  assert.equal(migrateDatabase(db),2);
  assert.equal(db.prepare("SELECT COUNT(*) AS n FROM schema_migrations").get().n,2);
  for(const table of ["providers","provider_accounts","provider_sessions","courses","course_chapters","lectures","live_classes","entitlement_checks","playback_sessions","provider_content_metadata"]) {
    assert.ok(db.prepare("SELECT name FROM sqlite_master WHERE type='table' AND name=?").get(table),table);
  }
  db.close();
});
test("migration preserves existing user, subject and class rows",()=>{
  const db=new Database(":memory:");
  db.exec(`
    CREATE TABLE users (id TEXT PRIMARY KEY,name TEXT NOT NULL,email TEXT NOT NULL UNIQUE COLLATE NOCASE,password_hash TEXT NOT NULL,created_at TEXT NOT NULL);
    CREATE TABLE subjects (id TEXT PRIMARY KEY,name TEXT NOT NULL UNIQUE);
    CREATE TABLE chapters (id TEXT PRIMARY KEY,subject_id TEXT NOT NULL REFERENCES subjects(id),name TEXT NOT NULL);
    CREATE TABLE classes (id TEXT PRIMARY KEY,title TEXT NOT NULL,subject TEXT NOT NULL,teacher TEXT NOT NULL,description TEXT,starts_at TEXT,status TEXT NOT NULL,type TEXT NOT NULL CHECK(type IN ('live','recorded')),join_url TEXT,playback_url TEXT,visibility TEXT NOT NULL DEFAULT 'paid' CHECK(visibility IN ('free','paid')),provider TEXT NOT NULL DEFAULT 'first_party',created_at TEXT NOT NULL);
    CREATE TABLE class_entitlements (user_id TEXT NOT NULL,class_id TEXT NOT NULL,PRIMARY KEY(user_id,class_id));
    CREATE TABLE schedules (id TEXT PRIMARY KEY,class_id TEXT NOT NULL,starts_at TEXT NOT NULL,ends_at TEXT);
    INSERT INTO users VALUES('u1','Existing','existing@example.com','salt:hash','2025-01-01');
    INSERT INTO subjects VALUES('b1','Existing batch');
    INSERT INTO classes(id,title,subject,teacher,status,type,created_at) VALUES('c1','Existing class','Existing batch','Teacher','ready','recorded','2025-01-01');
  `);
  assert.equal(migrateDatabase(db),2);
  assert.equal(db.prepare("SELECT name FROM users WHERE id='u1'").get().name,"Existing");
  assert.equal(db.prepare("SELECT name FROM subjects WHERE id='b1'").get().name,"Existing batch");
  assert.equal(db.prepare("SELECT title FROM classes WHERE id='c1'").get().title,"Existing class");
  assert.equal(db.prepare("SELECT demo_access FROM classes WHERE id='c1'").get().demo_access,0);
  assert.equal(db.prepare("SELECT chapter_id FROM classes WHERE id='c1'").get().chapter_id,null);
  db.close();
});
