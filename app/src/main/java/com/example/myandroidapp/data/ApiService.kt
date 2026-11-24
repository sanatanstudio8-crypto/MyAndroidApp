package com.example.myandroidapp.data
import android.content.Context
import com.example.myandroidapp.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface LearningApi {
    @GET("v1/classes/live") suspend fun liveClasses(): List<ClassDto>
    @GET("v1/classes/recorded") suspend fun recordedClasses(): List<ClassDto>
    @GET("v1/subjects") suspend fun subjects(): List<SubjectDto>
    @GET("v1/subjects/{subjectId}/chapters") suspend fun chapters(@Path("subjectId") subjectId: String): List<ChapterDto>
    @GET("v1/schedule") suspend fun schedule(): List<ScheduleDto>
}
object ApiServiceFactory {
    fun learning(context: Context): LearningApi {
        val base=BuildConfig.API_BASE_URL.trim()
        require(base.startsWith("https://")) { "A configured HTTPS catalogue API URL is required." }
        return Retrofit.Builder().baseUrl(if(base.endsWith("/")) base else "$base/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(LearningApi::class.java)
    }
}
