# MyAndroidApp API

This is the app's own learning-data service. It is not an RWA endpoint proxy and does not contain copied RWA material.

## Run locally
1. Install Node.js 20 or newer.
2. Copy `.env.example` to `.env`.
3. Set `JWT_SECRET` to a random secret with at least 32 characters.
4. Set `DATABASE_URL` to use PostgreSQL, or leave it unset and set `DATABASE_FILE` for SQLite. PostgreSQL is selected whenever `DATABASE_URL` is present. For production, use a managed PostgreSQL connection string from protected environment configuration.
5. Run `npm install`, then `npm start`.

The API binds to localhost by default. Set `HOST=0.0.0.0` behind a properly configured HTTPS reverse proxy. Do not expose the Node process directly to the public internet.

## Isolated demo mode
- Opt in to the Android sample UI at compile time with `-PDEMO_MODE=true`; the default is `false`.
- Demo mode displays clearly labelled sample Free, Premium, recorded and live-class cards without loading real-provider catalogue data.
- Demo buttons show an explanatory notice only. They do not open a player or a live meeting.
- Backend demo routes are available only when server-side `DEMO_MODE=true`. They return synthetic metadata and `demo_granted` / `demo_preview` markers, never a media URL.
- Demo grants are not written to `entitlement_checks` and are not consulted by real entitlement or playback endpoints.
- The legacy `classes.demo_access` flag is display metadata only; it never exposes Premium join or playback URLs.
- Demo entitlements are not Original RWA authorization and must never be described as real Premium access.

## Content administration
The database starts empty. Populate it only through a trusted administrative process after verifying ownership/licensing and playback rights. Real Premium playback requires an independent real-provider entitlement decision and a provider-issued session. Keep provider credentials server-side. No demo route changes real entitlement records.

## Security
- Login/signup endpoints remain available for future non-demo use, but are not required by the current Android demo.
- Premium media links are withheld unless the class has the explicit demo flag.
- Keep provider credentials and private storage credentials on the server, never in the APK.
- Use persistent database storage in production; ephemeral filesystems can lose catalogue data on restart.

## Database compatibility

- PostgreSQL runtime uses the asynchronous `pg` pool, PostgreSQL-specific versioned migrations, and async providers.
- SQLite remains supported when `DATABASE_URL` is not set; its existing migrations and synchronous providers remain in place.
- GitHub Actions runs the SQLite API/migration tests and PostgreSQL integration tests against an isolated PostgreSQL 16 service container.
- PostgreSQL integration tests cover migrations/idempotency, case-insensitive email uniqueness and login, catalogue reads, foreign-key enforcement, and unknown-entitlement denial.
