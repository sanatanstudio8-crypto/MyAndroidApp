# SQLite to PostgreSQL persistence assessment

## Decision

Do not switch the running API to PostgreSQL in this change. The current backend is built around the synchronous `better-sqlite3` API. Its request handlers and providers call `prepare(...).get/all/run` synchronously, and `createDatabase` applies migrations through SQLite's `exec` and transaction APIs. PostgreSQL's Node client (`pg`) is asynchronous and has a different transaction and query interface. A safe cutover therefore requires a deliberate database-interface refactor, not a connection-string change.

This assessment is documentation only. It does not add a PostgreSQL driver, create a database, modify Render, change production environment variables, or move any data.

## Existing database and migration architecture

- `backend/src/server.js` opens `DATABASE_FILE` (default `./data/app.sqlite`) with `better-sqlite3`.
- `createDatabase` enables SQLite WAL and foreign keys, then calls `migrateDatabase`.
- `backend/src/db/migrations.js` stores applied versions in `schema_migrations`. Migrations 1 and 2 run in SQLite transactions and are skipped when their version is already recorded.
- Migration 1 creates/retains the legacy `users`, `subjects`, `chapters`, `classes`, `class_entitlements`, and `schedules` tables and conditionally adds legacy `classes` columns.
- Migration 2 creates provider, course, lecture, entitlement, and playback tables and indexes.
- `sqlite-providers.js` and legacy routes issue SQLite statements directly. The provider interfaces are not yet a database abstraction.

## PostgreSQL compatibility

The logical relational model is portable, but the existing migration code is not directly executable on PostgreSQL.

| Existing SQLite feature | PostgreSQL treatment required |
| --- | --- |
| `PRAGMA table_info(classes)` | Query `information_schema.columns` or PostgreSQL catalogs |
| `INTEGER` flags constrained to 0/1 | Use `BOOLEAN` and map API values explicitly, or retain integer with explicit checks |
| `COLLATE NOCASE` on email | Use a normalized lowercase email column/constraint or PostgreSQL `citext` after deciding extension policy |
| SQLite `?` placeholders | PostgreSQL `$1`, `$2`, ... |
| `better-sqlite3` synchronous `prepare/get/all/run` | Async pool/client API; refactor request handlers and providers to async |
| SQLite `db.transaction(fn)` | Explicit PostgreSQL `BEGIN` / `COMMIT` / `ROLLBACK` on one checked-out client |
| `db.exec` multi-statement calls | Execute PostgreSQL-compatible migration statements in a controlled transaction |

Foreign keys, primary keys, unique constraints, ordinary indexes, text identifiers, timestamps stored as text, and the relational table structure have straightforward PostgreSQL equivalents. SQLite's dynamic typing means actual stored values still need validation before import.

## Safest migration path

1. **Build a database boundary first.** Define async query, transaction, and close operations. Refactor all routes and providers to use it while keeping SQLite behavior covered by existing tests.
2. **Keep migration history immutable.** Do not rewrite or renumber SQLite migrations 1 and 2. Add a PostgreSQL-specific migration set with matching logical schema and explicit dialect/version metadata. Record PostgreSQL-applied versions in its own `schema_migrations` table. Never mark a PostgreSQL migration applied until its transaction commits.
3. **Test PostgreSQL in CI.** Add a PostgreSQL service container to GitHub Actions and integration tests for clean schema creation, repeat/idempotent migration, foreign keys, uniqueness/case handling, transaction rollback, CRUD/provider behavior, and compatibility of API DTOs.
4. **Prepare a non-destructive export/import tool.** Read from a consistent SQLite snapshot; export every existing table, preserving IDs and nullable values; import in foreign-key dependency order inside PostgreSQL transactions. Preserve password hashes verbatim. Do not copy provider secrets or playback URLs into logs. Do not delete or mutate the source SQLite file.
5. **Validate before cutover.** Compare table row counts, primary-key sets, null counts, uniqueness constraints, foreign-key integrity, and representative API responses. Record a checksum/manifest and retain a restorable SQLite backup.
6. **Stage first, then cut over only with approval.** Test on a separate staging database. For production, schedule a write freeze, take and verify a final backup, import, run read-only validation, then explicitly approve switching the API's database setting. Keep the original SQLite backup untouched for rollback. Do not dual-write without a separately designed consistency protocol.

## Render configuration findings

The connected Render web service is currently on the Free plan in Singapore, and no Render Postgres instances were returned for the workspace. A Free web service can connect to a Render Postgres database, but a Free Render Postgres database is not suitable for production durability: Render documents a 1 GB limit, 30-day expiry, no managed backups, and possible restarts/maintenance. The current Free web service also cannot attach a persistent disk.

The service and database must be in the same workspace and region to use Render's internal database URL. Keep the URL/password in Render environment variables or secret management; never commit them or print them in logs.

## Cost and durability notes

Render's current public pricing page lists paid Postgres starting at **$6/month** for the smallest listed paid tier, with **1 GB storage included** and additional storage listed at **$0.30/GB/month**. Prices and plan availability can change; confirm the final tier and region in the Render Dashboard before purchase. The Free Postgres option is $0 but expires after 30 days and lacks managed backups, so it is only appropriate for temporary evaluation, not production data.

## Known risks and unresolved inputs

- The live Render service is still configured to deploy from `feature/auth-backend-player-foundation`, not this working branch. This change does not alter that setting.
- The service's SQLite file is on an ephemeral filesystem. The current service inspection showed no persistent disk. Its actual live database contents could not be inspected through the available Render service metadata, so do not assume it is empty.
- No SQLite database file or export was available in the repository inspection. Existing production data volume and exact legacy schema therefore remain unverified.
- PostgreSQL application runtime support and an actual PostgreSQL integration test suite do not exist yet.
- The current migration tests cover SQLite only.

## Explicit remaining steps

- Approve the async database-interface refactor and PostgreSQL driver dependency.
- Approve creating a paid Render Postgres instance after reviewing the exact dashboard price/region/backup configuration.
- Supply the database connection only through Render's protected environment configuration; no credential should be shared in chat or committed.
- Obtain a consistent SQLite backup/export from the actual source database before any import.
- Implement and pass PostgreSQL integration tests in CI.
- Complete staging import and data-integrity comparison.
- Obtain separate approval for production cutover. Until then, production persistence is **not fixed**.
