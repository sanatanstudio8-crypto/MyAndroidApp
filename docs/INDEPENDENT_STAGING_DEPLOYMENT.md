# Independent staging deployment

This deployment is isolated from the existing production API and the older demo staging service.

## Render service

- Service name: `balidan-independent-staging-api`
- Git branch: `work/balidan-independent-staging`
- Root directory: repository root
- Runtime: Node
- Build command: `cd backend && npm install`
- Start command: `cd backend && npm start`
- Region: Singapore
- Plan: Free (subject to Render free-instance availability)
- Health endpoint: `/health`

## Required Render environment

Set these values in the new service only:

- `REQUIRE_POSTGRES=true`
- `DEMO_MODE=false`
- `PGSSLMODE=require`
- `JWT_SECRET`: generate a unique random secret of at least 32 characters in the Render dashboard
- `DATABASE_URL`: select the **internal** connection string for the Render database `balidan-rwa-independent-staging-db` and store it as a secret environment variable
- `CORS_ORIGINS`: the approved Android app origin(s), or leave empty when no browser origin is used

Never copy the database URL or JWT secret into Git, issue comments, CI output, logs, or this document. Do not use an external database URL.

The server now refuses to start in required-PostgreSQL mode if `DATABASE_URL` is missing or is not a PostgreSQL URL. It will not silently create a local SQLite database in staging.

## Database and health verification

On first successful startup, the backend applies the versioned PostgreSQL migrations. Then verify:

1. `GET /health` reports `database: postgres`.
2. The `schema_migrations` table contains the latest migration version.
3. `/v1/batches`, `/v1/courses`, `/v1/courses/:courseId/chapters`, `/v1/chapters/:chapterId/lectures` return only records present in the independent database.
4. No sample/demo catalogue is exposed as Original RWA content.
5. Authenticated entitlement checks deny missing, expired and unknown grants.
6. Playback remains unavailable unless an authorized media provider returns a valid HTTPS session.
7. Live joining remains unavailable unless an authorized live provider returns a valid join session.

The Render MCP SQL query integration cannot currently reach this database because its IP allowlist is empty and the hosted query runner has no allowlistable source IP. Keep the allowlist closed; verify migrations from the backend's private Render network or a separately approved secure database client instead.

## Original RWA boundary

Original RWA integration remains disabled until written authorization, official API/session/entitlement/DRM/live documentation and an approved sandbox are supplied. APK route-name strings are not an API contract. Do not implement OTP, subscription, payment, DRM, Widevine, PallyCon or signed-session bypasses.

## Android build

Build the app against the independent backend with:

```sh
./gradlew assembleDebug -PAPI_BASE_URL=https://balidan-independent-staging-api.onrender.com
```

The Gradle default now points to the independent staging URL. Existing UI and navigation are unchanged.
