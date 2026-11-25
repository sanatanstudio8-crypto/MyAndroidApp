# Balidan RWA backend API contract

This is the stable Balidan-owned API. It is not an Original RWA API specification. No Original RWA endpoints, credentials, tokens, media URLs or DRM configuration are assumed.

## Catalogue (public)
- GET /v1/batches -> array of { id, title, name }
- GET /v1/courses?batchId={id} -> array of { id, title, description?, visibility, batchId? }
- GET /v1/courses/{courseId} -> course object or 404
- GET /v1/courses/{courseId}/chapters -> array of { id, courseId, title, position }
- GET /v1/chapters/{chapterId}/lectures -> recorded lecture metadata array { id, chapterId, title, description?, type, visibility }
- GET /v1/live-classes -> array of { id, title, courseId?, startsAt?, status, visibility }

The legacy /v1/classes/live, /v1/classes/recorded, /v1/subjects, /v1/subjects/{subjectId}/chapters and /v1/schedule routes remain available for existing UI compatibility. The migration does not seed catalogue records.

## Identity and protected operations
- POST /v1/auth/signup and POST /v1/auth/login retain the Bearer JWT response.
- GET /v1/entitlements/content/{contentId} requires a valid Bearer token and returns { contentId, decision }, where decision is granted, denied or unknown.
- POST /v1/playback/sessions requires a valid Bearer token and { contentId }. Unknown or denied entitlement returns 403. Playback provider absence returns 503 and never returns a fabricated URL.

## Isolated demo mode
- Android demo UI is opt-in at compile time with `-PDEMO_MODE=true`; default is false.
- `GET /v1/demo/catalogue`, `GET /v1/demo/entitlements/content/{contentId}` and `POST /v1/demo/playback/sessions` exist only when the server is started with `DEMO_MODE=true`.
- Demo content IDs use the `demo-` prefix. Demo entitlements are marked `demo_granted`; preview sessions are marked `demo_preview`, have no playback URL and never call the real PlaybackProvider.
- Demo decisions are not stored in real entitlement tables and are not accepted by `/v1/playback/sessions`. The real endpoint still requires a Bearer token and a separate real entitlement decision.
- Legacy `classes.demo_access` is metadata only and never reveals Premium media or meeting URLs.

## Provider boundary
CatalogueProvider, IdentityProvider, EntitlementProvider and PlaybackProvider are server-side interfaces. Provider credentials and secret references must remain server-side. A provider must independently confirm entitlement before issuing authorized playback. Unknown is not authorization. The default playback provider is intentionally unconfigured.

## SQLite
Versioned migrations are tracked in schema_migrations and applied transactionally. Existing legacy tables and rows are retained; missing legacy columns are added with safe defaults. Provider tables are created empty. Configure DATABASE_FILE on persistent storage for production; ephemeral filesystems are not durable.

## Security
Only content owned by Balidan or explicitly licensed for it may be catalogued. Do not bypass third-party login, payment, subscription, entitlement or DRM.
