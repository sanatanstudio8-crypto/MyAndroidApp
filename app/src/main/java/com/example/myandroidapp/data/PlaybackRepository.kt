package com.example.myandroidapp.data

class PlaybackRepository(private val api: LearningApi) {
    suspend fun entitlement(contentId: String): EntitlementDto = api.entitlement(contentId)

    suspend fun createSession(contentId: String): PlaybackSessionDto {
        require(contentId.isNotBlank()) { "A lecture content ID is required." }
        val entitlement = api.entitlement(contentId)
        require(entitlement.contentId == contentId && entitlement.decision == "granted") {
            "Content entitlement is not granted."
        }
        return api.createPlaybackSession(PlaybackSessionRequest(contentId)).also {
            require(it.contentId == contentId && it.status == "created" &&
                it.playbackUrl?.startsWith("https://", ignoreCase = true) == true
            ) { "The server did not issue an authorized playback session." }
        }
    }

    suspend fun refreshSession(sessionId: String, contentId: String): PlaybackSessionDto {
        require(sessionId.isNotBlank() && contentId.isNotBlank()) { "A playback session and lecture are required." }
        val entitlement = api.entitlement(contentId)
        require(entitlement.contentId == contentId && entitlement.decision == "granted") {
            "Content entitlement is no longer granted."
        }
        return api.refreshPlaybackSession(sessionId).also {
            require(it.id == sessionId && it.contentId == contentId && it.status == "created" &&
                it.playbackUrl?.startsWith("https://", ignoreCase = true) == true
            ) { "The server did not refresh an authorized playback session." }
        }
    }

    suspend fun closeSession(sessionId: String): PlaybackSessionCloseDto {
        require(sessionId.isNotBlank()) { "A playback session ID is required." }
        return api.closePlaybackSession(sessionId).also {
            require(it.sessionId == sessionId && it.status == "closed") {
                "The server did not confirm playback session closure."
            }
        }
    }
}
