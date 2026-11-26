# Balidan RWA API

This is Balidan's own learning-data service. It is not an Original RWA endpoint proxy and contains no copied Original RWA material.

## Run locally

1. Install Node.js 20 or newer.
2. Copy `.env.example` to `.env`.
3. Set `JWT_SECRET` to a random secret with at least 32 characters.
4. Set `DATABASE_URL` to use PostgreSQL, or leave it unset and set `DATABASE_FILE` for SQLite.
5. For the isolated sample experience only, set `DEMO_MODE=true`.
6. Run `npm install`, then `npm start`.

The API binds to localhost by default. For a device test, expose it only through a properly configured HTTPS development endpoint. Do not expose the Node process directly to the public internet.

## Isolated demo mode

- Opt in to the Android sample UI at compile time with `-PDEMO_MODE=true`; the default is `false`.
- The backend must independently run with `DEMO_MODE=true`. A client build flag alone never enables server demo routes or grants access.
- `GET /v1/demo/catalogue` returns synthetic Free and Premium batches, courses, chapters, lectures and live-room listings.
- `POST /v1/demo/playback/sessions` checks a server-side demo entitlement fixture and returns a media reference, never a playback URL. The Android app maps only the two known demo media references to the public MDN CC0 flower test clip and shows Media3 controls.
- Free and Premium sample recordings use the same public CC0 test clip. They are demonstrations of the authorization flow, not real course recordings or protected Premium media.
- `POST /v1/demo/live/sessions` checks a separate server-side demo entitlement and creates an in-process local demo-room membership. `POST /v1/demo/live/sessions/:liveClassId/leave` closes that membership. This is a local room simulation, not a live broadcast or external meeting.
- Demo fixtures include granted, denied, expired and unknown cases. The client cannot submit or change an entitlement decision.
- Demo entitlement and session providers are separate from the real Balidan entitlement and playback providers. They never write real entitlement records or call Original RWA.
- Demo routes return 404 unless the backend process has `DEMO_MODE=true`.

The demo media is a public CC0 test asset, so it is not confidential and must never be used as evidence of real Premium content protection. Demo authorization controls the app's sample flow only; it is not Original RWA authorization.

## Content administration

The database starts empty. Populate it only through a trusted administrative process after verifying ownership/licensing and playback rights. Real Premium playback requires an independent real-provider entitlement decision and a provider-issued session. Keep provider credentials server-side. No demo route changes real entitlement records.

## Security

- Real provider adapters remain disabled until written provider authorization, official integration documentation and a sandbox are supplied.
- Keep provider credentials and private storage credentials on the server, never in the APK.
- Demo entitlement cannot grant real provider playback or live joining.
- Use persistent database storage in production; ephemeral filesystems can lose catalogue data on restart.
- HTTPS is required for the Android API base URL.

## Database compatibility

- PostgreSQL runtime uses the asynchronous `pg` pool, PostgreSQL-specific versioned migrations and async providers.
- SQLite remains supported when `DATABASE_URL` is not set; its existing migrations and synchronous providers remain in place.
- GitHub Actions runs SQLite API/migration tests and PostgreSQL integration tests against an isolated PostgreSQL 16 service container.
