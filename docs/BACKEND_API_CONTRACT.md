# MyAndroidApp backend API contract

This is the API contract for **our own backend**, not an RWA API specification. It is a client-side integration target and must be implemented/deployed before login or class loading can work.

## Transport and security
- HTTPS only; the Android client rejects a non-HTTPS base URL.
- JSON request and response bodies.
- Login/signup return a short-lived bearer access token.
- Send the token in `Authorization: Bearer <token>`.
- Keep provider/RWA credentials only on the server. Never ship them in the APK.
- Enforce account authorization and course entitlements on every class and playback request.
- Return expiring, authorized playback/join URLs. Do not return protected media unless the provider has granted redistribution/embedding rights.

## Endpoints

### POST /v1/auth/signup
Request: `{ "name": "...", "email": "...", "password": "..." }`
Response: `{ "accessToken": "...", "tokenType": "Bearer" }`

### POST /v1/auth/login
Request: `{ "email": "...", "password": "..." }`
Response: `{ "accessToken": "...", "tokenType": "Bearer" }`

### GET /v1/classes/live
Authenticated. Returns a JSON array of class objects.

### GET /v1/classes/recorded
Authenticated and entitlement-checked. Returns a JSON array of class objects.

Class object fields: `id`, `title`, `subject`, `teacher`, optional `description`, optional `startsAt`, `status`, optional `joinUrl`, optional `playbackUrl`.

### GET /v1/subjects
Authenticated. Returns `[{ "id": "...", "name": "..." }]`.

### GET /v1/subjects/{subjectId}/chapters
Authenticated. Returns `[{ "id": "...", "subjectId": "...", "name": "..." }]`.

### GET /v1/schedule
Authenticated. Returns `[{ "id": "...", "classId": "...", "startsAt": "...", "endsAt": "..." }]`.

## RWA provider integration
RWA data must not be populated until RWA supplies written authorization, API documentation, credentials and permitted playback terms. Until then the backend should return empty arrays for RWA-backed classes. No sample lectures are to be inserted.
