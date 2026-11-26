package com.example.myandroidapp.data

class LearningRepository(private val api: LearningApi) {
    suspend fun batches() = api.batches()
    suspend fun courses(batchId: String? = null) = api.courses(batchId)
    suspend fun course(courseId: String) = api.course(courseId)
    suspend fun chapters(courseId: String) = api.courseChapters(courseId)
    suspend fun recordedLectures(chapterId: String) = api.lectures(chapterId)
    suspend fun liveClasses() = api.liveLearningClasses()
}
