# Original RWA integration status

## Current state

The application currently talks to Balidan's own API. It is not an Original RWA API client or proxy. The real Original RWA adapters are intentionally disabled. Do not treat the synthetic DEMO_MODE catalogue, the public MDN CC0 sample clip, or the in-process demo live room as Original RWA content.

## Implemented independently

- Android Retrofit declarations exist for the Balidan API catalogue, authentication, entitlement and playback-session endpoints.
- The Android API client attaches a locally stored Balidan bearer token when one exists.
- The Android token store encrypts a token with Android Keystore AES-GCM.
- The Android Media3 player accepts HTTPS URLs supplied by the authorized backend.
- The backend has normalized, transport-independent RWA provider interfaces and DTO validators.
- Backend API routes enforce Balidan JWT authentication and require a positive entitlement result before asking the playback provider to create a session.
- The backend rejects playback sessions without a created status and HTTPS playback URL.
- The real RWA identity, catalogue, entitlement, playback and live-join implementations remain disabled by design.
- The demo providers are isolated and do not grant real-provider access.

## Exact provider inputs required

Obtain these from Original RWA through an authorized provider/partner contact:

1. Written authorization covering catalogue display, Free/Premium playback, live-class joining, the intended user population, and any caching/deep-link restrictions.
2. Official integration documentation and API version, including base URLs, environments, rate limits, pagination, error codes and support contact.
3. A provider-approved sandbox/test tenant and test accounts for Free, Premium, expired, denied and revoked states.
4. Authentication contract: supported login/SSO flow, client registration, OAuth/OIDC details if applicable, redirect URIs, token types/scopes, expiry, refresh, revocation, MFA and account-linking rules. No provider password or token should be embedded in the APK.
5. Catalogue contract: batch, course, chapter, recorded lecture and live-class endpoints; exact request/response schemas; stable identifiers; pagination; sorting; availability/status; visibility; metadata; and update/webhook or sync rules.
6. Entitlement contract: authoritative account/course/lecture checks, subscription state, Free/Premium rules, expiry, region/device limits, concurrency limits, and required re-check timing.
7. Recorded playback contract: official lecture-details/session-create/session-refresh/session-close APIs, signed URL lifetime, supported HLS/DASH/MP4 formats, required headers/cookies, CDN allowlists, subtitle/audio tracks, seek rules, and session concurrency.
8. DRM contract for each protected tier: approved DRM system (for example Widevine if explicitly specified), license-server URL and request/response format, certificate/provisioning requirements, security level, key rotation, offline policy, and device/output restrictions. DRM vendor/product names must be confirmed by the provider; none is assumed here.
9. Live-class contract: official provider SDK/API, meeting platform, join-token/session issuance and refresh, waiting-room/host controls, schedule/time-zone fields, attendance callbacks, leave/reconnect behavior, and supported Android SDK/version requirements.
10. Branding, attribution, privacy, telemetry, data-retention and any contractual restrictions on showing provider content inside a third-party app.

## Integration gates

- Do not implement guessed endpoints, request fields, response schemas, signing algorithms, token formats, URLs or DRM configuration.
- Do not scrape the Original RWA app, extract private media URLs, reuse another user's session, bypass login/payment checks, or disable DRM.
- Keep provider credentials server-side in a secret manager/environment; never package them in Android resources, BuildConfig or the APK.
- Map provider responses into the existing normalized DTOs only after the official contract is available.
- Add contract tests using provider sandbox fixtures, then run authorized end-to-end tests for catalogue, entitlement, playback and live joining.
- Keep the existing Original RWA-style Android UI unchanged while wiring it to the normalized Balidan API.

## Current blockers

No written Original RWA authorization, official API/SDK documentation, provider sandbox credentials, account-linking/auth contract, catalogue schema, entitlement contract, playback/DRM contract or live-join contract is present in this repository's checked-in documentation/configuration. Consequently, no genuine Original RWA content can currently be fetched or played through this codebase.
