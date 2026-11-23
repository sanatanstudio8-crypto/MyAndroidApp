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

interface AuthApi {
    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("v1/auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse
}

interface LearningApi {
    @GET("v1/classes/live")
    suspend fun liveClasses(): List<ClassDto>

    @GET("v1/classes/recorded")
    suspend fun recordedClasses(): List<ClassDto>

    @GET("v1/subjects")
    suspend fun subjects(): List<SubjectDto>

    @GET("v1/subjects/{subjectId}/chapters")
    suspend fun chapters(@Path("subjectId") subjectId: String): List<ChapterDto>

    @GET("v1/schedule")
    suspend fun schedule(): List<ScheduleDto>
}

object ApiServiceFactory {
    private fun retrofit(context: Context): Retrofit {
        val tokenStore = SecureTokenStore(context.applicationContext)
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    tokenStore.readToken()?.let { token ->
                        header("Authorization", "Bearer $token")
                    }
                    header("Accept", "application/json")
                }.build()
                chain.proceed(request)
            }
            .build()

        val apiBaseUrl = BuildConfig.API_BASE_URL.trim()
        require(apiBaseUrl.isNotBlank()) {
            "Backend is not configured yet. The app needs the deployed HTTPS API URL."
        }
        require(apiBaseUrl.startsWith("https://")) {
            "The backend URL must use HTTPS."
        }

        return Retrofit.Builder()
            .baseUrl(if (apiBaseUrl.endsWith("/")) apiBaseUrl else "$apiBaseUrl/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun auth(context: Context): AuthApi = retrofit(context).create(AuthApi::class.java)
    fun learning(context: Context): LearningApi = retrofit(context).create(LearningApi::class.java)
}
