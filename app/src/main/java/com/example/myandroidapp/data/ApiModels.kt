package com.example.myandroidapp.data

data class LoginRequest(val email: String, val password: String)
data class SignupRequest(val name: String, val email: String, val password: String)
data class AuthResponse(val accessToken: String, val tokenType: String = "Bearer")

data class ClassDto(
    val id: String,
    val title: String,
    val subject: String,
    val teacher: String,
    val description: String? = null,
    val startsAt: String? = null,
    val status: String,
    val joinUrl: String? = null,
    val playbackUrl: String? = null
)

data class SubjectDto(val id: String, val name: String)
data class ChapterDto(val id: String, val subjectId: String, val name: String)
data class ScheduleDto(
    val id: String,
    val classId: String,
    val startsAt: String,
    val endsAt: String? = null
)
