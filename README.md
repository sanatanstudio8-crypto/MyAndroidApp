# MyAndroidApp

Native Android learning app built with Kotlin and Jetpack Compose.

## Current development branch

`feature/auth-backend-player-foundation`

## Implemented foundation
- Login and signup screens wired to the app's own backend API contract.
- Access token encryption using Android Keystore-backed AES-GCM.
- Retrofit/OkHttp API client with bearer-token injection and HTTPS-only base URL validation.
- API models and client methods for live classes, recorded classes, subjects, chapters and schedules.
- Media3 player component for HTTPS media URLs issued by an authorized backend.
- Removed placeholder/sample lectures. Class lists remain empty until real authorized backend data is supplied.

## Backend
The Node.js API implementation is in [backend/](backend/README.md). It provides account registration/login, JWT authentication, entitlement-checked class lists, subjects, chapters and schedules. Its database starts empty and must be deployed on persistent storage behind HTTPS before the Android app can use it.\n\n## Not yet connected
- A deployed backend URL and server implementation.
- RWA API/partnership credentials, written permission and playback authorization.
- Real class list fetching and end-to-end playback.
- Device/CI build and APK verification.

## Configure backend URL
Pass an HTTPS Gradle property when building:

```bash
./gradlew assembleDebug -PAPI_BASE_URL=https://your-backend.example/
```

The API contract is documented in [docs/BACKEND_API_CONTRACT.md](docs/BACKEND_API_CONTRACT.md). The listed routes are for **our own backend**, not discovered RWA endpoints.

## Security
Never put RWA credentials, signing secrets, or private API keys in the Android app. Keep them server-side. Only return class and playback data that the account is authorized to access and that the provider permits to be embedded.
