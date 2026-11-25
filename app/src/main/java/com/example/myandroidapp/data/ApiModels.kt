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
