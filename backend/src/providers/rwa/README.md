# Original RWA provider integration boundary

Status: **disabled**. This module contains normalized contracts and a disabled default only. It performs no network requests and does not encode an Original RWA request/response contract.

## Static APK references mapped to normalized interfaces

The following strings were observed in client APK static inspection. They are route/model *references only*, not verified live endpoints, response schemas, authorization rules, or proof that a route is currently available.

| Proposed interface | Reported static route/model references | Verification |
|---|---|---|
| RwaIdentityProvider | `get/sendotp`, `get/sendotpv2`, `get/otpverify`, `get/otpverifywithoutuser`, `post/userLogin`, `post/userSignup`, `post/social_login`, `post/user_sessions`, `get/check_user_exist`, `get/checkemailforresetpassword` | Unverified |
| RwaCatalogueProvider | `get/courselist`, `get/courselistnewv2`, `get/course_by_id`, `get/coursenew_by_idv2`, `get/course_tree`, `get/mycourse`, `get/mydemocourse`, `get/freecourselist`, `get/freecourselistnew`, `get/courselistsubscription`; model names `CourseModel`, `CourseTreeResponse`, `CourseTreeCourse`, `CourseResponseModel`, `MyCourseModel`, `MyCourseStudyModel`, `ChapterData`, `FolderCourseModel` | Unverified |
| RwaEntitlementProvider | `get/canviewvideo`, `get/mycourse`, `get/courselistsubscription`, `get/eligible_items`, `get/canUserUpgradeCourse`, `get/get_all_purchases`; model names `PurchasedCourseModel`, `MyPurchaseModel`, `TestPassSubscriptionDataModel`, `CourseSubscriptionModel` | Unverified |
| RwaPlaybackProvider | `get/fetchVideoDetailsById`, `get/course_class_videosv2`, `get/course_class_freecontentv2`, `get/getHlsQualtiesFromMediaId`, `get/get_mpd_drm_links`, `get/get_drm_peristent_token`, `post/generate_browserstream_token`, `post/generate_key_session`; model names `VideoDetailsResponseModel`, `AllRecordModel`, `MpdDrmLinksResponse`, `DrmPersistentTokenResponse` | Unverified |
| LiveJoinProvider | `get/live_class`, `get/live_classv2`, `get/live_upcoming_course_classv2`, `get/live_upcoming_free_course_classv2`, `get/live_video_details`, `get/video_qualities_live_video`, `get/get_previous_live_videos`; model names `LiveVideoDetailsResponseModel`, `LivestreamLink`, `HlsQualityModel` | Unverified |

Other recorded/live route references include `get/recorded_upcoming_course_classv2`, `get/live_upcoming_recorded_course_classv2`, `get/recent_classesv2`, `get/getVideoCompletion`, `get/get_video_timestamps`, `get/drip-progress`, and `get/pdfWatermarkConfig`. They are not assigned a request contract here.

Payment/subscription references such as `post/createorderv2`, `post/purchaseapiv5`, `post/generatePaymentLink`, `post/createOrderForCart`, `post/createOrderForStudyPass`, `post/submit_order`, `post/add_free_purchase`, `get/checkPaymentStatusForQRCode`, `post/createQRCodeForPayment`, `get/get_all_purchases`, `get/eligible_items`, and `get/canUserUpgradeCourse` are not implemented or called. Authentication, payment, subscription, and DRM are not bypassed.

## Normalized boundary

- Identity returns an account-session DTO containing only a token reference, never a provider bearer token.
- Catalogue returns normalized batches, courses, chapters, lectures, and live classes.
- Entitlements use only `granted`, `denied`, or `unknown`; unknown is never treated as granted.
- Recorded playback and live joining have separate interfaces and separate session DTOs.
- Mock sessions have no playable media URL or join URL and always set `providerAuthorization: false`.
- The real provider defaults throw `PROVIDER_NOT_CONFIGURED`. No environment variable can silently enable a real transport.

A production adapter requires official provider documentation, written authorization, a provider-approved sandbox/account, documented session and entitlement semantics, and authorized playback/DRM integration tests. No Original RWA URL, credential, token, payload, or response schema is inferred from APK strings.
