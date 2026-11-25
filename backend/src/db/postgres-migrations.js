const migrations = [
  {
    version: 1,
    name: "legacy_catalogue",
    sql: [
      "CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, name TEXT NOT NULL, email TEXT NOT NULL, password_hash TEXT NOT NULL, created_at TEXT NOT NULL)",
      "CREATE UNIQUE INDEX IF NOT EXISTS users_email_lower_unique ON users (LOWER(email))",
      "CREATE TABLE IF NOT EXISTS subjects (id TEXT PRIMARY KEY, name TEXT NOT NULL UNIQUE)",
      "CREATE TABLE IF NOT EXISTS chapters (id TEXT PRIMARY KEY, subject_id TEXT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE, name TEXT NOT NULL)",
      "CREATE TABLE IF NOT EXISTS classes (id TEXT PRIMARY KEY, title TEXT NOT NULL, subject TEXT NOT NULL, teacher TEXT NOT NULL, description TEXT, starts_at TEXT, status TEXT NOT NULL, type TEXT NOT NULL CHECK(type IN ('live','recorded')), join_url TEXT, playback_url TEXT, visibility TEXT NOT NULL DEFAULT 'paid' CHECK(visibility IN ('free','paid')), demo_access BOOLEAN NOT NULL DEFAULT FALSE, chapter_id TEXT, provider TEXT NOT NULL DEFAULT 'first_party', created_at TEXT NOT NULL)",
      "ALTER TABLE classes ADD COLUMN IF NOT EXISTS demo_access BOOLEAN NOT NULL DEFAULT FALSE",
      "ALTER TABLE classes ADD COLUMN IF NOT EXISTS chapter_id TEXT",
      "ALTER TABLE classes ADD COLUMN IF NOT EXISTS provider TEXT NOT NULL DEFAULT 'first_party'",
      "CREATE TABLE IF NOT EXISTS class_entitlements (user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, class_id TEXT NOT NULL REFERENCES classes(id) ON DELETE CASCADE, PRIMARY KEY(user_id,class_id))",
      "CREATE TABLE IF NOT EXISTS schedules (id TEXT PRIMARY KEY, class_id TEXT NOT NULL REFERENCES classes(id) ON DELETE CASCADE, starts_at TEXT NOT NULL, ends_at TEXT)"
    ]
  },
  {
    version: 2,
    name: "provider_catalogue_and_sessions",
    sql: [
      "CREATE TABLE IF NOT EXISTS providers (id TEXT PRIMARY KEY, key TEXT NOT NULL UNIQUE, enabled BOOLEAN NOT NULL DEFAULT FALSE, created_at TEXT NOT NULL)",
      "CREATE TABLE IF NOT EXISTS provider_accounts (id TEXT PRIMARY KEY, provider_id TEXT NOT NULL REFERENCES providers(id), user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, external_account_id TEXT, created_at TEXT NOT NULL, UNIQUE(provider_id,user_id))",
      "CREATE TABLE IF NOT EXISTS provider_sessions (id TEXT PRIMARY KEY, provider_id TEXT NOT NULL REFERENCES providers(id), user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, secret_ref TEXT, expires_at TEXT, created_at TEXT NOT NULL)",
      "CREATE TABLE IF NOT EXISTS courses (id TEXT PRIMARY KEY, provider_id TEXT REFERENCES providers(id), external_id TEXT, batch_id TEXT, title TEXT NOT NULL, description TEXT, visibility TEXT NOT NULL DEFAULT 'paid' CHECK(visibility IN ('free','paid')), created_at TEXT NOT NULL)",
      "CREATE TABLE IF NOT EXISTS course_chapters (id TEXT PRIMARY KEY, course_id TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE, external_id TEXT, title TEXT NOT NULL, position INTEGER NOT NULL DEFAULT 0)",
      "CREATE TABLE IF NOT EXISTS lectures (id TEXT PRIMARY KEY, chapter_id TEXT NOT NULL REFERENCES course_chapters(id) ON DELETE CASCADE, provider_id TEXT REFERENCES providers(id), external_id TEXT, title TEXT NOT NULL, description TEXT, lecture_type TEXT NOT NULL DEFAULT 'recorded' CHECK(lecture_type IN ('recorded','live')), visibility TEXT NOT NULL DEFAULT 'paid' CHECK(visibility IN ('free','paid')), created_at TEXT NOT NULL)",
      "CREATE TABLE IF NOT EXISTS live_classes (id TEXT PRIMARY KEY, provider_id TEXT REFERENCES providers(id), external_id TEXT, course_id TEXT REFERENCES courses(id), title TEXT NOT NULL, starts_at TEXT, status TEXT NOT NULL DEFAULT 'scheduled', visibility TEXT NOT NULL DEFAULT 'paid' CHECK(visibility IN ('free','paid')))",
      "CREATE TABLE IF NOT EXISTS entitlement_checks (id TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, content_id TEXT NOT NULL, decision TEXT NOT NULL CHECK(decision IN ('granted','denied','unknown')), checked_at TEXT NOT NULL, expires_at TEXT)",
      "CREATE TABLE IF NOT EXISTS playback_sessions (id TEXT PRIMARY KEY, user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE, content_id TEXT NOT NULL, provider_id TEXT REFERENCES providers(id), status TEXT NOT NULL CHECK(status IN ('created','denied','expired')), protocol TEXT, playback_url TEXT, expires_at TEXT, created_at TEXT NOT NULL)",
      "CREATE TABLE IF NOT EXISTS provider_content_metadata (provider_id TEXT NOT NULL REFERENCES providers(id), content_type TEXT NOT NULL, content_id TEXT NOT NULL, metadata_json TEXT NOT NULL DEFAULT '{}', PRIMARY KEY(provider_id,content_type,content_id))",
      "CREATE INDEX IF NOT EXISTS idx_courses_batch ON courses(batch_id)",
      "CREATE INDEX IF NOT EXISTS idx_course_chapters_course ON course_chapters(course_id,position)",
      "CREATE INDEX IF NOT EXISTS idx_lectures_chapter ON lectures(chapter_id,lecture_type)",
      "CREATE INDEX IF NOT EXISTS idx_live_classes_start ON live_classes(starts_at)",
      "CREATE INDEX IF NOT EXISTS idx_entitlement_user_content ON entitlement_checks(user_id,content_id,checked_at)"
    ]
  }
];

export async function migratePostgres(db) {
  await db.exec("CREATE TABLE IF NOT EXISTS schema_migrations (version INTEGER PRIMARY KEY, name TEXT NOT NULL, applied_at TEXT NOT NULL)");
  const applied = new Set((await db.all("SELECT version FROM schema_migrations")).map(row => Number(row.version)));
  for (const migration of migrations) {
    if (applied.has(migration.version)) continue;
    const client = await db.pool.connect();
    try {
      await client.query("BEGIN");
      for (const statement of migration.sql) await client.query(statement);
      await client.query("INSERT INTO schema_migrations(version,name,applied_at) VALUES($1,$2,$3)", [migration.version, migration.name, new Date().toISOString()]);
      await client.query("COMMIT");
    } catch (error) {
      await client.query("ROLLBACK");
      throw error;
    } finally {
      client.release();
    }
  }
  const row = await db.get("SELECT MAX(version) AS version FROM schema_migrations");
  return Number(row?.version || 0);
}
