# Balidan RWA

Native Android learning demo built with Kotlin, Jetpack Compose and Media3, backed by Balidan's own Node.js API.

## Isolated demo

The opt-in demo has separate Free and Premium sample batches, courses, chapters, recorded lessons and live-room cards. It requires both:

- Android compile flag: `-PDEMO_MODE=true`
- Backend environment: `DEMO_MODE=true`

Recorded demo sessions are authorized by a server-side fixture before the Android app maps the returned media reference to the public MDN CC0 flower test video. The player uses Media3 controls. Both sample tiers use test media only; they are not Original RWA lectures or protected Premium content.

Free and Premium live cards join a server-managed, in-process local demo room. The app provides a Leave action. This is a room lifecycle simulation, not a live broadcast or external meeting.

The backend fixture tests granted, denied, expired and unknown decisions. The Android client cannot choose the entitlement decision. Demo providers are separate from real entitlement/playback providers and cannot grant Original RWA access.

See [backend/README.md](backend/README.md) for local setup and the demo API contract.

## Configure the Android API

Pass an HTTPS API base URL when compiling:

```bash
./gradlew :app:compileDebugKotlin -PDEMO_MODE=true -PAPI_BASE_URL=https://your-demo-api.example/
```

Do not put provider credentials or signing secrets in the APK. Real Original RWA adapters remain disabled until written authorization, official integration documentation and a provider-approved sandbox are available.

## Validation

GitHub Actions compiles the Android debug and opt-in Demo Mode Kotlin variants and runs backend SQLite and PostgreSQL tests. The workflow compiles Kotlin; it does not create an APK artifact.
