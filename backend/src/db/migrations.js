export const migrations = [
  {
    version: 1,
    name: "legacy_catalogue",
    up(db) {
      db.exec(`
        CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY,name TEXT NOT NULL,email TEXT NOT NULL UNIQUE COLLATE NOCASE,password_hash TEXT NOT NULL,created_at TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS subjects (id TEXT PRIMARY KEY,name TEXT NOT NULL UNIQUE);
        CREATE TABLE IF NOT EXISTS chapters (id TEXT PRIMARY KEY,subject_id TEXT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,name TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS classes (id TEXT PRIMARY KEY,title TEXT NOT NULL,subject TEXT NOT NULL,teacher TEXT NOT NULL,description TEXT,starts_at TEXT,status TEXT NOT NULL,type TEXT NOT NULL CHECK(type IN ('live','recorded')),join_url TEXT,playback_url TEXT,visibility TEXT NOT NULL DEFAULT 'paid' CHECK(visibility IN ('free','paid')),demo_access INTEGER NOT NULL DEFAULT 0 CHECK(demo_access IN (0,1)),chapter_id TEXT,provider TEXT NOT NULL DEFAULT 'first_party',created_at TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS class_entitlements (user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,class_id TEXT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,PRIMARY KEY(user_id,class_id));
        CREATE TABLE IF NOT EXISTS schedules (id TEXT PRIMARY KEY,class_id TEXT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,starts_at TEXT NOT NULL,ends_at TEXT);
      `);
      const columns = db.prepare("PRAGMA table_info(classes)").all().map(c => c.name);
      if (!columns.includes("demo_access")) db.exec("ALTER TABLE classes ADD COLUMN demo_access INTEGER NOT NULL DEFAULT 0 CHECK(demo_access IN (0,1))");
      if (!columns.includes("chapter_id")) db.exec("ALTER TABLE classes ADD COLUMN chapter_id TEXT");
      if (!columns.includes("provider")) db.exec("ALTER TABLE classes ADD COLUMN provider TEXT NOT NULL DEFAULT 'first_party'");
    }
  },
  {
    version: 2,
    name: "provider_catalogue_and_sessions",
    up(db) {
      db.exec(`
        CREATE TABLE IF NOT EXISTS providers (id TEXT PRIMARY KEY, key TEXT NOT NULL UNIQUE, enabled INTEGER NOT NULL DEFAULT 0 CHECK(enabled IN (0,1)), created_at TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS provider_accounts (id TEXT PRIMARY KEY, provider_id TEXT NOT NULL REFERENCES providers(id), user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, external_account_id TEXT, created_at TEXT NOT NULL, UNIQUE(provider_id,user_id));
        CREATE TABLE IF NOT EXISTS provider_sessions (id TEXT PRIMARY KEY, provider_id TEXT NOT NULL REFERENCES providers(id), user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, secret_ref TEXT, expires_at TEXT, created_at TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS courses (id TEXT PRIMARY KEY, provider_id TEXT REFERENCES providers(id), external_id TEXT, batch_id TEXT, title TEXT NOT NULL, description TEXT, visibility TEXT NOT NULL DEFAULT 'paid' CHECK(visibility IN ('free','paid')), created_at TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS course_chapters (id TEXT PRIMARY KEY, course_id TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE, external_id TEXT, title TEXT NOT NULL, position INTEGER NOT NULL DEFAULT 0);
        CREATE TABLE IF NOT EXISTS lectures (id TEXT PRIMARY KEY, chapter_id TEXT NOT NULL REFERENCES course_chapters(id) ON DELETE CASCADE, provider_id TEXT REFERENCES providers(id), external_id TEXT, title TEXT NOT NULL, description TEXT, lecture_type TEXT NOT NULL DEFAULT 'recorded' CHECK(lecture_type IN ('recorded','live')), visibility TEXT NOT NULL DEFAULT 'paid' CHECK(visibility IN ('free','paid')), created_at TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS live_classes (id TEXT PRIMARY KEY, provider_id TEXT REFERENCES providers(id), external_id TEXT, course_id TEXT REFERENCES courses(id), title TEXT NOT NULL, starts_at TEXT, status TEXT NOT NULL DEFAULT 'scheduled', visibility TEXT NOT NULL DEFAULT 'paid' CHECK(visibility IN ('free','paid')));
        CREATE TABLE IF NOT EXISTS entitlement_checks (id TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, content_id TEXT NOT NULL, decision TEXT NOT NULL CHECK(decision IN ('granted','denied','unknown')), checked_at TEXT NOT NULL, expires_at TEXT);
        CREATE TABLE IF NOT EXISTS playback_sessions (id TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, content_id TEXT NOT NULL, provider_id TEXT REFERENCES providers(id), status TEXT NOT NULL CHECK(status IN ('created','denied','expired')), protocol TEXT, playback_url TEXT, expires_at TEXT, created_at TEXT NOT NULL);
        CREATE TABLE IF NOT EXISTS provider_content_metadata (provider_id TEXT NOT NULL REFERENCES providers(id), content_type TEXT NOT NULL, content_id TEXT NOT NULL, metadata_json TEXT NOT NULL DEFAULT '{}', PRIMARY KEY(provider_id,content_type,content_id));
        CREATE INDEX IF NOT EXISTS idx_courses_batch ON courses(batch_id);
        CREATE INDEX IF NOT EXISTS idx_course_chapters_course ON course_chapters(course_id,position);
        CREATE INDEX IF NOT EXISTS idx_lectures_chapter ON lectures(chapter_id,lecture_type);
        CREATE INDEX IF NOT EXISTS idx_live_classes_start ON live_classes(starts_at);
        CREATE INDEX IF NOT EXISTS idx_entitlement_user_content ON entitlement_checks(user_id,content_id,checked_at);
      `);
    }
  }
];

export function migrateDatabase(db) {
  db.exec("CREATE TABLE IF NOT EXISTS schema_migrations (version INTEGER PRIMARY KEY, name TEXT NOT NULL, applied_at TEXT NOT NULL)");
  const applied = new Set(db.prepare("SELECT version FROM schema_migrations").all().map(row => row.version));
  for (const migration of migrations) {
    if (applied.has(migration.version)) continue;
    const apply = db.transaction(() => {
      migration.up(db);
      db.prepare("INSERT INTO schema_migrations(version,name,applied_at) VALUES(?,?,?)")
        .run(migration.version, migration.name, new Date().toISOString());
    });
    apply();
  }
  return db.prepare("SELECT MAX(version) AS version FROM schema_migrations").get().version || 0;
}
