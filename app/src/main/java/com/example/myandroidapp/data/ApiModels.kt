package com.example.myandroidapp.data

data class LoginRequest(val email: String, val password: String)
data class SignupRequest(val name: String, val email: String, val password: String)
data class AuthResponse(val accessToken: String, val tokenType: String = "Bearer")

data class ClassDto(
    val id: String, val title: String, val subject: String, val teacher: String,
    val description: String? = null, val startsAt: String? = null, val status: String,
    val joinUrl: String? = null, val playbackUrl: String? = null,
    val visibility: String = "free", val demoAccess: Boolean = false, val chapterId: String? = null
)
data class SubjectDto(val id: String, val name: String)
data class ChapterDto(val id: String, val subjectId: String, val name: String)
data class ScheduleDto(val id: String, val classId: String, val startsAt: String, val endsAt: String? = null)

data class BatchDto(val id: String, val title: String, val name: String = title)
data class CourseDto(val id: String, val title: String, val description: String? = null, val visibility: String = "paid", val batchId: String? = null)
data class CourseChapterDto(val id: String, val courseId: String, val title: String, val position: Int = 0)
data class LectureDto(val id: String, val chapterId: String, val title: String, val description: String? = null, val type: String = "recorded", val visibility: String = "paid")
data class LiveClassDto(val id: String, val title: String, val courseId: String? = null, val startsAt: String? = null, val status: String, val visibility: String = "paid")
data class EntitlementDto(val contentId: String, val decision: String)
data class PlaybackSessionRequest(val contentId: String)
data class PlaybackSessionDto(val id: String, val contentId: String, val status: String, val protocol: String? = null, val playbackUrl: String? = null, val expiresAt: String? = null)
data class PlaybackSessionCloseDto(val sessionId: String, val status: String)

data class DemoCatalogueDto(
    val mode: String,
    val notice: String,
    val batches: List<BatchDto>,
    val courses: List<CourseDto>,
    val chapters: List<CourseChapterDto>,
    val lectures: List<LectureDto>,
    val liveClasses: List<LiveClassDto>
)
data class DemoPlaybackSessionRequest(val contentId: String)
data class DemoPlaybackSessionDto(
    val id: String,
    val contentId: String,
    val status: String,
    val mediaRef: String?,
    val expiresAt: String?,
    val source: String,
    val providerAuthorization: Boolean
)
data class DemoLiveJoinRequest(val liveClassId: String)
data class DemoLiveJoinSessionDto(
    val id: String,
    val liveClassId: String,
    val title: String,
    val status: String,
    val roomType: String,
    val joinedAt: String,
    val expiresAt: String,
    val source: String,
    val providerAuthorization: Boolean
)
data class DemoLiveLeaveDto(
    val liveClassId: String,
    val status: String,
    val sessionClosed: Boolean,
    val source: String
)

data class CatalogueBatchDto(val id: String, val title: String, val name: String = title, val courses: List<CatalogueCourseDto> = emptyList())
data class CatalogueCourseDto(val id: String, val title: String, val description: String? = null, val visibility: String = "paid", val batchId: String? = null, val chapters: List<CatalogueChapterDto> = emptyList())
data class CatalogueChapterDto(val id: String, val courseId: String, val title: String, val position: Int = 0, val lectures: List<CatalogueLectureDto> = emptyList())
data class CatalogueLectureDto(val id: String, val chapterId: String, val title: String, val description: String? = null, val type: String = "recorded", val visibility: String = "paid")
data class CataloguePageDto(
    val items: List<CatalogueBatchDto> = emptyList(),
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasNextPage: Boolean,
    val liveClasses: List<LiveClassDto> = emptyList(),
    val legacyLive: List<ClassDto> = emptyList(),
    val legacyRecorded: List<ClassDto> = emptyList(),
    val subjects: List<SubjectDto> = emptyList(),
    val schedules: List<ScheduleDto> = emptyList()
)
data class CatalogueSnapshotDto(
    val batches: List<CatalogueBatchDto>,
    val liveClasses: List<LiveClassDto>,
    val legacyLive: List<ClassDto>,
    val legacyRecorded: List<ClassDto>,
    val subjects: List<SubjectDto>,
    val schedules: List<ScheduleDto>
)
