# Provider-independent Original RWA adapter

This module prepares the integration boundary without claiming that an Original RWA connection exists.

## Adapter layout

`src/providers/rwa/provider-independent-adapter.js` exports `ProviderIndependentRwaAdapter`. It exposes five capability groups:

- `adapter`: authentication, refresh, validation and revocation
- `adapter.catalogue`: batches, courses, course detail, chapters, recorded lectures and live-class catalogue
- `adapter.entitlement`: account, course, lecture and subscription checks
- `adapter.playback`: lecture detail, recorded playback session create/refresh/close
- `adapter.liveJoin`: live-class detail and join-session create/refresh/leave

Transport operations and response normalizers are injected. The adapter contains no provider endpoint paths, credentials, request field names, token assumptions, media URLs, DRM configuration or meeting protocol. Official response payloads must first be mapped by the injected normalizers to the existing normalized DTO input fields in `dtos.js`.

## Required provider-supplied contracts

Before enabling the adapter, obtain all of the following from Original RWA through an authorized channel:

1. Written authorization, permitted use, environments, account-linking policy and security requirements.
2. Versioned official API/SDK documentation and a provider-issued sandbox/test account.
3. Authentication: exact operation, request/response schemas, MFA/device flow if applicable, token storage/expiry/refresh/revocation and account identity mapping.
4. Catalogue: pagination, filters, stable IDs and official schemas for batch/course/chapter/lecture/live-class relationships, ordering and availability.
5. Entitlement: authoritative account/course/lecture/subscription checks, Free/Premium rules, expiry, denial and unknown/error semantics.
6. Recorded playback: official session creation/renewal/closure, authorized playback response, expiry and concurrency rules.
7. DRM: supported DRM system(s), license URL/contract, required headers or challenge flow, key/session renewal and Android SDK/player requirements. Do not infer these from a media URL.
8. Live joining: official provider SDK/protocol, join credentials, scheduling/status schema, refresh and leave semantics, and any Android SDK requirements.
9. Test fixtures and expected success, denied, expired, revoked, malformed and transient-error responses.

## Placeholders and configuration

`ORIGINAL_RWA_INTEGRATION_PLACEHOLDERS` names the required documentation, authorization, sandbox, operation mapping, normalization, DRM and live-protocol inputs. Fill these only in a private integration module from official documentation. Keep provider secrets server-side; never put them in Android resources, BuildConfig, source control or APK assets.

Use `configurationStatus` to inspect missing operation and normalizer callbacks. The adapter deliberately throws `ProviderNotConfiguredError` for missing callbacks. It must not be wired as a production provider until the authorization and contract checklist is complete and sandbox contract tests pass.

## Integration boundary

The current Balidan server continues using its existing local providers and disabled Original RWA providers. This adapter is not wired into `createApp`, does not alter production configuration, and does not create access to Original RWA content. Once the official contracts arrive, a separately reviewed composition module can inject the approved transport/normalizers into the server provider slots.

Playback DTO normalization rejects non-HTTPS URLs, but that check is not DRM implementation or proof of provider authorization. A provider-issued session must be verified server-side before any playback data is returned. Live join credentials and URLs must likewise come only from an authorized provider session.

## Server dependency injection (disabled by default)

`createApp({ providers: { originalRwa } })` accepts an explicit provider bundle. The server exposes the selected bundle at `app.locals.originalRwaProviders` for controlled backend composition. It accepts the injected bundle only when both `enabled === true` and `configurationStatus.ready === true`; otherwise it installs the disabled provider implementations. No Original RWA routes are activated by this wiring, and existing Balidan/demo routes continue using their existing providers.

For a future authorized integration, create the adapter in a server-only composition module (not in the Android app), supply the official operation callbacks and normalizers there, then pass `adapter.toProviderBundle({ enabled: true })` to `createApp`. The adapter returns `enabled: true` only when every required operation and normalizer exists. The server must remain configured without this bundle until sandbox verification and security review are complete. Never load provider secrets from client build arguments or commit them to the repository.
