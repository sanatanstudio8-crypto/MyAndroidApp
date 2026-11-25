package com.example.myandroidapp.data

class PlaybackRepository(private val api: LearningApi) {
    suspend fun entitlement(contentId: String): EntitlementDto = api.entitlement(contentId)
    suspend fun createSession(contentId: String): PlaybackSessionDto {
        val entitlement = api.entitlement(contentId)
        require(entitlement.decision == "granted") { "Content entitlement is not granted." }
        return api.createPlaybackSession(PlaybackSessionRequest(contentId))
    }
}
