# MyAndroidApp API

This is the app's own learning-data service. It is not an RWA endpoint proxy and does not contain copied RWA material.

## Run locally
1. Install Node.js 20 or newer.
2. Copy `.env.example` to `.env`.
3. Set `JWT_SECRET` to a random secret with at least 32 characters.
4. Set `DATABASE_FILE` to a persistent disk path for production.
5. Run `npm install`, then `npm start`.

The API binds to localhost by default. Set `HOST=0.0.0.0` behind a properly configured HTTPS reverse proxy. Do not expose the Node process directly to the public internet.

## College demo mode
- The Android demo opens directly to the public lecture catalogue; no account login is required.
- The catalogue displays both Free and Premium lecture metadata.
- Premium rows are labelled in the app. A Premium video URL is returned only when an administrator explicitly sets `demo_access=1` for that class.
- Free content and Premium Demo content must be owned by the project or separately licensed for this demonstration.
- Do not copy RWA content or bypass any third-party login, payment, entitlement, or DRM mechanism.

## Content administration
The database starts empty. Populate it only through a trusted administrative process after verifying ownership/licensing and demo playback rights. For a Premium class, set `visibility='paid'`; set `demo_access=1` only for content specifically authorized for this demo. Keep media URLs HTTPS and point them to authorized demonstration hosting. No public endpoint changes these flags.

## Security
- Login/signup endpoints remain available for future non-demo use, but are not required by the current Android demo.
- Premium media links are withheld unless the class has the explicit demo flag.
- Keep provider credentials and private storage credentials on the server, never in the APK.
- Use persistent database storage in production; ephemeral filesystems can lose catalogue data on restart.
