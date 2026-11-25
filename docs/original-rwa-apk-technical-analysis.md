# Original RWA APK — Static Technical Analysis & Authorized Integration Map

**Analysis artifact:** `Rojgar With Ankit.apk`  
**APK SHA-256:** `839ce6ea5dfc5957b7e09e95ff6909786facbe01f0dd47e4477a1d5e01a646c4`  
**Manifest package:** `com.appx.rojgar_with_ankit`  
**Application class:** `com.appx.core.Appx`  
**Version:** `11.0.3` (versionCode `1103`)  
**SDK metadata:** minSdk 24, targetSdk 36, compileSdk 36  
**Reference Balidan revision:** `854c54755805ec8c0ac201d19ed4db00d03be2bc` on `work/balidan-postgres-validation`

> This is a static inspection report. It records manifest declarations, DEX class/type/string references, and resource names. It does not claim that any referenced API is currently reachable, that any response schema is known, or that playback/live joining was runtime-tested. The APK's package and embedded Play Store listing identify this particular artifact as Rojgar With Ankit. If a different Original RWA APK was intended, this report must be repeated against that exact file.

## 1. Inspection scope and confidence

Inspected:
- Binary Android manifest: package/version/SDK metadata, declared permissions, activities, services, receivers and providers.
- Four DEX files: class/type names and static string constants, including route-like strings, media/DRM markers and model names.
- APK resource inventory: player, course, folder, recorded-video and live-class layouts/drawables.
- Existing Balidan provider contracts, DTOs, disabled adapters, provider README and server wiring at the reference branch.

Not performed:
- No APK installation or runtime execution.
- No login, OTP, purchase, subscription, playback or live-class session.
- No HTTP interception, packet capture, API probing or endpoint invocation.
- No DRM/license exchange, key extraction, stream download or protected-media access.
- No source-level decompilation/control-flow reconstruction. The findings below are class and string references, not a complete recovered implementation.

**Confidence labels**
- **Static-confirmed:** present in the inspected APK manifest, DEX string/class tables or resources.
- **Architecture inference:** a reasonable mapping from class/model names to Balidan interfaces, not an observed runtime contract.
- **Runtime-unconfirmed:** requires provider documentation, authorized test credentials/sandbox or an approved runtime test.

## 2. APK identity, manifest and permissions

The manifest declares package `com.appx.rojgar_with_ankit`, application class `com.appx.core.Appx`, version 11.0.3 / 1103, min SDK 24 and target SDK 36. DEX strings include a Play Store listing for the same package and the product name “Rojgar With Ankit”.

Static manifest permission declarations relevant to learning/live/media features include:
- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE` and `ACCESS_WIFI_STATE`
- `android.permission.WAKE_LOCK`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.RECORD_AUDIO`, `CAMERA`, `MODIFY_AUDIO_SETTINGS`
- `BLUETOOTH` and `BLUETOOTH_CONNECT`
- Legacy `READ_EXTERNAL_STORAGE` (max SDK 32) and `WRITE_EXTERNAL_STORAGE` (max SDK 28)
- Other app features also declare telephony/call, NFC, alarm, vibration and billing permissions.

The manifest declares player/course/live activities including:
- Catalogue/course: `BatchesActivity`, `AllCourseActivity`, `CourseActivity`, `CourseDetailActivity`, `CourseExploreActivity`, `FolderCourseDetailActivity`, `FolderCoursesActivity`, `FolderCoursesContentsActivity`, `FolderChapterLayer`, `FreeCourseActivity`, `PaidCourseRecordActivity`.
- Recorded/player: `ExoPlayerDrmActivity`, `CustomExoPlayerActivity`, `ExoActivity`, `Exo2Activity`, `HlsOfflinePlayerActivity`, `PlayerRecordYoutube2Activity`, `WebViewPlayerActivityNew`.
- Live: `LivePlayer1Activity`, `LivePlayer2Activity`, `ExoLiveActivity`, `LivePaidActivity`, `LiveInteractiveActivity`, `NavigationLiveClassActivity`, `WebViewPlayerActivityLiveNew`, `WebinarPlayerActivity`.

Declared media download services include `com.appx.core.service.PallyConDownloadService` and `com.doverunner.widevine.service.DrmDownloadService`.

**Balidan permission decision:** do not copy the reference manifest wholesale. At present the provider documentation does not specify a Balidan permission set. INTERNET/network-state are ordinary prerequisites for a network-backed client; microphone/camera/Bluetooth/notifications/storage permissions should be requested only if the authorized live-class feature set actually needs them. The reference APK's declarations do not prove that every permission is required for Original RWA playback or live joining, nor that runtime permission grants were obtained.

## 3. Static catalogue and content graph

### 3.1 Batch and course discovery

Static references:
- Activities/fragments: `BatchesActivity`, `MyBatchesActivity`, `TeacherWiseBatches`, `BatchesTabFragment`, `AllCourseActivity`, `AllCourseFragment`, `CourseHomeFragment`, `CourseDetailFragment`, `CourseExploreActivity`, `CourseExploreRevampActivity`.
- Models: `ModelAllCourseData`, `ModelAllCourseResponse`, `Course`, `CourseModel`, `CourseResponseModel`, `CourseTreeCategory`, `CourseTreeCourse`, `CourseTreeResponse`, `MyCourseModel`, `MyCourseStudyModel`, `ModelPurchasedCourseData`, `ModelPurchasedCourseResponse`.
- Static route-like strings found in DEX: `get/courselist`, `get/courselistnewv2`, `get/course_by_id`, `get/coursenew_by_idv2`, `get/course_tree`, `get/mycourse`, `get/mydemocourse`, `get/freecourselist`, `get/freecourselistnew`, `get/courselistsubscription`.

The APK exposes batch/course screens and course-shaped model types. The DEX string table confirms route-name literals but does not establish HTTP verb semantics beyond the literal prefixes, parameter names, pagination, field types, response envelopes or whether each route remains active.

### 3.2 Chapters, topics and recorded lectures

Static references:
- Activities/fragments: `FolderChapterLayer`, `FolderCourseContentsActivity`, `FolderCourseDetailActivity`, `FreeCourseRecordActivity`, `FreeCourseTopicActivity`, `PaidCourseRecordActivity`, `PaidCourseTopicActivity`, `PaidCourseConceptActivity`, `FolderCourseContentsFragment`, `FolderCourseContentsFragment`.
- Models: `ChapterData`, `ChapterDataResponseModel`, `FolderCourseContentsModel`, `FolderCourseContentsResponseModel`, `AllTopicModel`, `AllTopicResponse`, `AllRecordModel`, `AllRecordResponse`, `RecordedUpcomingDataModel`, `RecordedUpcomingResponseModel`, `VideoDetailsResponseModel`, `Video`, `VideoUiModel`, `CourseModel`, `CourseTreeCourse`.
- ViewModels: `CourseViewModel`, `FolderCourseViewModel`, `FreeCoursesViewModel`, `RecordedViewModel`, `RecordedUpcomingViewModel`.
- Resource names include `activity_folder_courses_recorded.xml`, `activity_folder_course_detail.xml`, `folder_course_content_video.xml`, `folder_course_content_video_revamp.xml`, `folder_course_content_upcoming_video.xml`, `fragment_new_video_course.xml`, and player layouts.

Static route-like strings:
- `get/course_class_videosv2`
- `get/course_class_freecontentv2`
- `get/fetchVideoDetailsById`
- `get/recorded_upcoming_course_classv2`
- `get/live_upcoming_recorded_course_classv2`
- `get/recent_classesv2`
- `get/getVideoCompletion`
- `get/get_video_timestamps`
- `get/drip-progress`

The APK contains separate Free/Paid course/record screens and a free-content route literal. This supports a catalogue distinction, but **does not define the authoritative free-vs-premium entitlement rules**. In particular, a title, visibility flag or “free” route must never be treated as a grant without the provider's server-side entitlement decision.

### 3.3 Live classes

Static references:
- Activities/fragments: `LivePlayer1Activity`, `LivePlayer2Activity`, `ExoLiveActivity`, `LivePaidActivity`, `LiveInteractiveActivity`, `NavigationLiveClassActivity`, `PreviousLiveActivity`, `WebinarPlayerActivity`, `LiveClassesFragment`, `LiveClassFragment`, `NavigationLiveClassFragment`, `FragmentLiveClassesList`, `FragmentLiveClassYoutubePaid`, `FragmentLiveClassYoutubeUnpaid`.
- ViewModels: `LiveClassesViewModel`, `LiveStreamingViewModel`, `LiveUpcomingViewModel`, `NavigationLiveClassViewModel`, `FreeCoursesViewModel`.
- Models: `ModelLiveClassesData`, `ModelLiveClassesResponse`, `LiveClassSectionModel`, `LiveStreamModel`, `LiveVideoModel`, `LiveUpcomingModel`, `LiveUpcomingRecordedModel`, `LiveUpcomingResponse`, `PreviousLiveVideoResponseModel`, `NavigationLiveClassDataModel`, `NavigationLiveClassModel`, `LiveVideoDetailsResponseModel`, `LivestreamLink`, `HlsQualityModel`, `LiveInteractiveClassModel`, `LiveInteractiveGenerateTokenModel`, `LiveInteractiveTokenModel`.
- Resource names include `activity_exo_live.xml`, `activity_exo_live_new.xml`, `activity_live_player_1.xml`, `activity_live_player_2.xml`, `fragment_live_classes.xml`, `item_live_classes_row.xml`, `item_live_classes_row_revamp.xml`, and `fragment_navigation_live_class.xml`.

Static route-like strings:
- `get/live_class`, `get/live_classv2`
- `get/live_upcoming_course_classv2`, `get/live_upcoming_free_course_classv2`
- `get/live_video_details`, `get/video_qualities_live_video`
- `get/get_previous_live_videos`

The app has distinct live-class listing/player classes and live-video detail model names. This supports keeping live joining separate from recorded playback. It does not establish the live provider (for example, webinar, interactive classroom, HLS stream or other service) used for each class type.

## 4. Player and DRM architecture references

**Static-confirmed technology/class references:**
- AndroidX Media3 / ExoPlayer classes, including `androidx.media3.exoplayer.ExoPlayer`, `androidx.media3.ui.PlayerView` and Media3 DRM classes.
- Player activity variants: `ExoPlayerDrmActivity`, `CustomExoPlayerActivity`, `ExoActivity`, `Exo2Activity`, `ExoLiveActivity`, `HlsOfflinePlayerActivity`, `LivePlayer1Activity`, `LivePlayer2Activity`, plus WebView/YouTube player activities.
- Static media markers: `.m3u8`, `.mpd`, `drm`, `drmLicenseUrl`, `MpdDrmLinksResponse`, `DrmPersistentTokenData`, `DrmPersistentTokenResponse`, `get/get_mpd_drm_links`, `get/get_drm_peristent_token`, `post/generate_browserstream_token`, `post/generate_key_session`.
- DRM vendor references: `com.appx.core.service.PallyConDownloadService`, `com.doverunner.widevine.service.DrmDownloadService`, `com.doverunner.widevine.sdk.DrWvSDK`, and `com.doverunner.widevine.exception.WvLicenseServerException`.

**What this does and does not prove:** the APK statically includes Media3/ExoPlayer, HLS/DASH markers and PallyCon/DOVERUNNER Widevine-related components. It is not proof that every lecture uses DRM, that all media use one player, that a particular license URL is current, or that a client can lawfully create a playable session. No license exchange was attempted and no DRM material is reproduced here.

**Integration rule:** Balidan must receive an authorized playback session from the provider adapter and pass the authorized protocol/session configuration to a supported player. It must not construct stream or license URLs from static strings, derive a license request, reuse the reference app's private SDK configuration, extract keys, or treat a manifest/MPD URL as sufficient authorization. The provider must explicitly approve the SDK, license integration, device policy, offline/persistent-license behavior and test procedure.

## 5. Static route inventory and trust boundary

The following literals were checked against the DEX string tables in this APK. They are route-name evidence only—not an API specification.

| Domain | DEX route-name literals confirmed |
|---|---|
| Identity | `get/sendotp`, `get/sendotpv2`, `get/otpverify`, `get/otpverifywithoutuser`, `post/userLogin`, `post/userSignup`, `post/social_login`, `post/user_sessions`, `get/check_user_exist`, `get/checkemailforresetpassword` |
| Catalogue | `get/courselist`, `get/courselistnewv2`, `get/course_by_id`, `get/coursenew_by_idv2`, `get/course_tree`, `get/mycourse`, `get/mydemocourse`, `get/freecourselist`, `get/freecourselistnew`, `get/courselistsubscription` |
| Entitlement / purchases | `get/canviewvideo`, `get/eligible_items`, `get/canUserUpgradeCourse`, `get/get_all_purchases` |
| Recorded content / playback | `get/fetchVideoDetailsById`, `get/course_class_videosv2`, `get/course_class_freecontentv2`, `get/getHlsQualtiesFromMediaId`, `get/get_mpd_drm_links`, `get/get_drm_peristent_token`, `post/generate_browserstream_token`, `post/generate_key_session` |
| Live | `get/live_class`, `get/live_classv2`, `get/live_upcoming_course_classv2`, `get/live_upcoming_free_course_classv2`, `get/live_video_details`, `get/video_qualities_live_video`, `get/get_previous_live_videos` |
| Progress / history | `get/recorded_upcoming_course_classv2`, `get/live_upcoming_recorded_course_classv2`, `get/recent_classesv2`, `get/getVideoCompletion`, `get/get_video_timestamps`, `get/drip-progress`, `get/pdfWatermarkConfig` |
| Payment / subscription workflow | `post/createorderv2`, `post/purchaseapiv5`, `post/generatePaymentLink`, `post/createOrderForCart`, `post/createOrderForStudyPass`, `post/submit_order`, `post/add_free_purchase` |

The two strings `get/checkPaymentStatusForQRCode` and `post/createQRCodeForPayment` are **not** present in the scanned DEX string table for this artifact. Do not carry them forward as confirmed routes for this APK.

A base URL literal `https://rozgarapinew.teachx.in/` and a class web URL literal beginning `https://class.teachx.in?token=` are present. They are recorded as static strings, not endorsed production endpoints. Provider confirmation is required before any connection is made.

## 6. Mapping to existing Balidan RWA interfaces

No interface or implementation change is proposed by this analysis. The current normalized interfaces in `backend/src/providers/contracts.js` are sufficient as the adapter boundary.

| Balidan interface method | Static reference evidence | Mapping / unresolved item |
|---|---|---|
| `RwaIdentityProvider.authenticate` | OTP/login/social-login/session route literals; `Authentication`, `OTPSignInResponse`, `OtpLessLoginResponse`, `UserSessionModel` | Map only after official login/OTP request, response, MFA, error and session rules are supplied. |
| `refreshSession`, `validateSession`, `revokeSession` | `post/user_sessions`, `AuthenticationViewModel`, `UserSessionModel` | Exact refresh/revoke operations, expiry, rotation and revocation semantics are unknown. |
| `RwaCatalogueProvider.listBatches` | Batch activities/fragments; `get/courselist*`; `ModelAllCourseData`, `CourseModel` | No confirmed batch-specific route, query parameters or response envelope. Obtain official batch catalogue contract. |
| `listCourses`, `getCourse` | `get/courselist*`, `get/course_by_id`, `get/coursenew_by_idv2`, `get/course_tree`; course models | Determine canonical course ID, batch relation, paging, visibility, archive state and course detail schema. |
| `listChapters` | `ChapterData`, `ChapterDataResponseModel`, `FolderCourseContentsModel`, `AllTopicModel`; course/folder screens | Chapter/topic distinction and parent identifiers need provider confirmation. |
| `listLectures(chapterId, type)` | `AllRecordModel`, `AllTopicModel`, `VideoDetailsResponseModel`, Free/Paid Record activities; course-class video/free-content route literals | Determine whether the provider calls these topics, records, classes or videos; map recorded/live type only from authoritative fields. |
| `listLiveClasses` | Live class route literals; `ModelLiveClassesData`, `LiveClassSectionModel`, `LiveUpcomingModel` | Need schedule, timezone, state, class type, join eligibility and pagination schema. |
| `RwaEntitlementProvider.checkAccountAccess` | login/session models, `get/mycourse`, purchase models | Provider must define account-level free/premium status; catalogue visibility is not entitlement. |
| `checkCourseAccess`, `checkLectureAccess` | `get/canviewvideo`, `get/eligible_items`, purchase/subscription models | Need authoritative decision response, reason codes, expiry, refund/grace and product/course/lecture scope. |
| `getSubscriptionStatus` | `get/courselistsubscription`, `get/get_all_purchases`, `CourseSubscriptionModel`, `PurchasedCourseModel` | Need plan, active dates, renewal/cancel/expiry and entitlement precedence. |
| `RwaPlaybackProvider.getLectureDetails` | `get/fetchVideoDetailsById`, `VideoDetailsResponseModel`, `MpdDrmLinksResponse` | Need authorized metadata schema and server-side access check. |
| `createSession`, `refreshSession`, `closeSession` | HLS/DASH/DRM route literals; player/DRM classes | Need session lifecycle, signed URL/token policy, DRM license handoff and concurrency/device rules. Never infer these from route names. |
| `LiveJoinProvider.getLiveClassDetails` | `get/live_video_details`, `LiveVideoDetailsResponseModel`, `LivestreamLink` | Need provider/class-type discriminant, scheduled state and join eligibility. |
| `createJoinSession`, `refreshJoinSession`, `leaveSession` | live-class/player activities and live token model names | Need join-token issuance/expiry/refresh/leave contracts and provider-specific SDK requirements. Keep separate from recorded playback. |

### Existing architecture to preserve

- `RwaIdentityProvider`, `RwaCatalogueProvider`, `RwaEntitlementProvider`, and `RwaPlaybackProvider` remain separate.
- `LiveJoinProvider` remains separate from `RwaPlaybackProvider`.
- DTO normalization remains in `backend/src/providers/rwa/dtos.js`.
- Real provider adapters remain disabled in `backend/src/providers/rwa/disabled-rwa-providers.js` until explicit authorization and official contracts are supplied.
- Unknown entitlement remains fail-closed; demo data/entitlements are not Original RWA authorization.
- No mock or demo response may return a playable media URL or live join URL.

## 7. Official contracts and approvals still required

Before implementing a real adapter, obtain all of the following directly from the provider or its authorized technical contact:

1. **Written integration authorization:** permission to integrate Original RWA account/catalogue/entitlement services and launch recorded/live content from Balidan; supported app identity, bundle/package restrictions and production approval process.
2. **Official API specification:** OpenAPI/Postman or equivalent for the approved base URL, versions, HTTP methods, required headers, request/response JSON, pagination, rate limits, idempotency, error codes and deprecation policy. Static DEX strings are not enough.
3. **Authentication/session contract:** OTP delivery/verification, login factors, account linking, token type and storage requirements, expiry/refresh/rotation/revocation, logout, device binding, concurrent-session limits, account lockout and recovery.
4. **Catalogue contract:** canonical batch/course/chapter/topic/lecture/live-class identifiers; parent-child relationships; ordering; content types; localization; visibility; free/premium labels; pagination; availability windows; deleted/archived handling.
5. **Entitlement contract:** server-authoritative account/course/lecture access decisions; free-tier rules; subscription/product mapping; validity periods; expiry/grace/refund/revocation behavior; device limits; and reason codes. No client-side inference from `free`, `paid`, `purchased` or catalogue presence.
6. **Recorded playback contract:** authorized lecture-details/session creation/refresh/close operations; protocol support (HLS/DASH/etc.); signed URL/token expiry; playback concurrency; seek/progress reporting; download/offline policy; geo/device restrictions; and approved error handling.
7. **DRM vendor onboarding:** provider-approved PallyCon/DOVERUNNER integration documentation, licensed SDK/version, Widevine security/device requirements, license-server configuration delivered through the official channel, request headers/claims, persistent/offline-license rules, provisioning, rotation and authorized test vectors. No license endpoint or SDK secret should be reverse-engineered from the APK.
8. **Live-class contract:** live-class detail/list schema; scheduled/live/ended/cancelled states; timezone semantics; join token lifecycle; provider type (interactive/WebRTC/HLS/webinar/etc.); approved SDK or URL launch flow; audio/video permissions; attendance/leave callbacks; reconnect and recording behavior.
9. **Operational/legal requirements:** privacy/data-processing terms, retention, consent, logging, support escalation, sandbox accounts, test content, SLA, production credentials delivery and incident process.
10. **Permission matrix:** provider-confirmed Android permissions and feature rationale for Balidan's chosen live implementation. The reference manifest is not itself an approval or requirement list.

## 8. Runtime confirmation matrix

| Claim | Current status | What would confirm it |
|---|---|---|
| APK identity/version/manifest declarations | Static-confirmed | Already extracted from supplied APK. |
| Catalogue route-name literals and model class names | Static-confirmed | Already found in DEX strings/class tables. |
| API methods, payload fields and response shapes | Runtime-unconfirmed | Official API specification plus authorized sandbox contract tests. |
| OTP/login/session behavior | Runtime-unconfirmed | Provider-approved test account and documented flow. |
| Free vs Premium entitlements | Runtime-unconfirmed | Provider-authoritative entitlement fixtures and allow/deny/expiry tests. |
| Recorded playback protocol and session lifecycle | Runtime-unconfirmed | Authorized free and premium test lectures, provider-issued session, supported DRM test. |
| PallyCon/DOVERUNNER license integration | Static SDK references only | Provider/vendor-approved SDK guide, configuration and license test content. |
| Live join provider and token behavior | Runtime-unconfirmed | Authorized scheduled/live test class and provider-issued join session. |
| Actual runtime permissions needed by Balidan | Runtime-unconfirmed | Approved feature matrix and Android runtime tests on supported devices. |

## 9. Safe integration sequence (planning only)

1. Confirm that this exact APK is the intended Original RWA reference artifact and obtain written provider integration authorization.
2. Receive official contracts and sandbox credentials through the provider's approved channel.
3. Build schema fixtures from official docs; map external IDs and response DTOs to Balidan normalized DTOs without changing the app's existing provider boundaries.
4. Contract-test identity and catalogue reads first; keep account tokens secret and outside logs.
5. Contract-test server-authoritative free/premium entitlements, including denial and expiry, before any playback call.
6. Integrate recorded playback only through the provider-approved session and DRM workflow.
7. Integrate live-class details and join as a separate provider flow with separate token/session DTOs.
8. Add authorized integration tests and explicit failure behavior; only then request separate approval for production enablement.

**Current outcome:** technical mapping prepared; no real provider transport implemented or enabled. No login/payment/subscription/DRM protection was bypassed. No production configuration, app architecture, APK artifact, deployment or merge was changed by this analysis.
