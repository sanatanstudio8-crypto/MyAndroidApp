package com.example.myandroidapp.data

import android.content.Context
import com.example.myandroidapp.BuildConfig
import com.example.myandroidapp.security.SecureTokenStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LearningApi {
    @GET("v1/classes/live") suspend fun liveClasses(): List<ClassDto>
    @GET("v1/classes/recorded") suspend fun recordedClasses(): List<ClassDto>
    @GET("v1/subjects") suspend fun subjects(): List<SubjectDto>
    @GET("v1/subjects/{subjectId}/chapters") suspend fun chapters(@Path("subjectId") subjectId: String): List<ChapterDto>
    @GET("v1/schedule") suspend fun schedule(): List<ScheduleDto>
    @GET("v1/batches") suspend fun batches(): List<BatchDto>
    @GET("v1/courses") suspend fun courses(@Query("batchId") batchId: String? = null): List<CourseDto>
    @GET("v1/courses/{courseId}") suspend fun course(@Path("courseId") courseId: String): CourseDto
    @GET("v1/courses/{courseId}/chapters") suspend fun courseChapters(@Path("courseId") courseId: String): List<CourseChapterDto>
    @GET("v1/chapters/{chapterId}/lectures") suspend fun lectures(@Path("chapterId") chapterId: String): List<LectureDto>
    @GET("v1/live-classes") suspend fun liveLearningClasses(): List<LiveClassDto>
    @GET("v1/entitlements/content/{contentId}") suspend fun entitlement(@Path("contentId") contentId: String): EntitlementDto
    @POST("v1/playback/sessions") suspend fun createPlaybackSession(@Body request: PlaybackSessionRequest): PlaybackSessionDto
}
interface AuthApi {
    @POST("v1/auth/login") suspend fun login(@Body request: LoginRequest): AuthResponse
    @POST("v1/auth/signup") suspend fun signup(@Body request: SignupRequest): AuthResponse
}
object ApiServiceFactory {
    private fun retrofit(context: Context): Retrofit {
        val base = BuildConfig.API_BASE_URL.trim()
        require(base.startsWith("https://")) { "A configured HTTPS catalogue API URL is required." }
        val tokenStore = SecureTokenStore(context.applicationContext)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request().newBuilder().apply {
                tokenStore.readToken()?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
            }.build()
            chain.proceed(request)
        }.build()
        return Retrofit.Builder().baseUrl(if (base.endsWith("/")) base else "$base/")
            .client(client).addConverterFactory(GsonConverterFactory.create()).build()
    }
    fun learning(context: Context): LearningApi = retrofit(context).create(LearningApi::class.java)
    fun auth(context: Context): AuthApi = retrofit(context).create(AuthApi::class.java)
}
