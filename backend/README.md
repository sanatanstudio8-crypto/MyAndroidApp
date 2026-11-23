# MyAndroidApp API

This backend is the app's own authentication and learning-data service. It is not an RWA endpoint proxy and does not contain copied RWA material.

## Run locally
1. Install Node.js 20 or newer.
2. Copy `.env.example` to `.env`.
3. Set `JWT_SECRET` to a random secret with at least 32 characters.
4. Set `DATABASE_FILE` to a persistent disk path for production.
5. Run `npm install`, then `npm start`.

The API binds to localhost by default. Set `HOST=0.0.0.0` behind a properly configured HTTPS reverse proxy for deployment. Do not expose the Node process directly to the public internet.

## Security controls
- Passwords are hashed with Node's scrypt and unique salts.
- Login/signup are rate-limited.
- Access tokens are signed JWTs with a 30-minute lifetime.
- All learning endpoints require bearer authentication.
- Paid classes require an explicit user/class entitlement.
- The Android app expects HTTPS and stores its access token encrypted with Android Keystore.

## Content
The database starts empty intentionally. No demo lectures are inserted. Classes should only be added by a trusted administrative process after verifying ownership/licensing and user entitlements. RWA content must not be added until RWA grants written permission and provides the official integration method and playback rights.

## RWA provider adapter
No provider adapter is enabled. Once written authorization and official API documentation are received, implement a server-side adapter here. Keep provider credentials in server environment secrets; never send them to the Android client.
