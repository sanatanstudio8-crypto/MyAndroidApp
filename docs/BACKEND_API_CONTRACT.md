# MyAndroidApp backend API contract

This contract describes **our own backend**, not an RWA API specification.

## Demo-mode transport
- HTTPS only; the Android client rejects a non-HTTPS API base URL.
- The public college-demo catalogue does not require login.
- Class media URLs must be HTTPS and must point to authorized demonstration content.
- Third-party provider credentials and protected playback/license URLs must never be placed in the APK.

## GET /v1/classes/live and GET /v1/classes/recorded
Public endpoints return both Free and Premium class metadata.

Class object fields: `id`, `title`, `subject`, `teacher`, optional `description`, optional `startsAt`, `status`, optional `joinUrl`, optional `playbackUrl`, `visibility` (`free` or `paid`), and `demoAccess` (boolean).

For Premium classes, `playbackUrl` and `joinUrl` are returned only when `demoAccess=true`, backed by the server-side `demo_access=1` flag. Otherwise the Premium listing remains visible but no playable URL is provided. Free class URLs are returned as configured by the trusted content administrator.

## GET /v1/subjects
Returns `[{ "id": "...", "name": "..." }]`.

## GET /v1/subjects/{subjectId}/chapters
Returns `[{ "id": "...", "subjectId": "...", "name": "..." }]`.

## GET /v1/schedule
Returns the schedule for classes in the demo catalogue.

## Authorization boundary
The app does not implement payment or subscription flows for this college demonstration. This does not grant access to third-party paid content. Only content owned by the project or explicitly licensed for the demo may be entered into the backend. Do not proxy or bypass RWA DRM, login, payment, or entitlement verification.
