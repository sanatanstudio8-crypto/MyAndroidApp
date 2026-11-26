package com.example.myandroidapp.data

import com.example.myandroidapp.security.SecureTokenStore

/** Auth operations persist only the short-lived app token in Android Keystore-backed storage. */
class AuthRepository(
    private val api: AuthApi,
    private val tokenStore: SecureTokenStore
) {
    suspend fun login(email: String, password: String): AuthResponse =
        api.login(LoginRequest(email.trim(), password)).also { persist(it) }

    suspend fun signup(name: String, email: String, password: String): AuthResponse =
        api.signup(SignupRequest(name.trim(), email.trim(), password)).also { persist(it) }

    fun logout() = tokenStore.clear()

    fun hasSession(): Boolean = !tokenStore.readToken().isNullOrBlank()

    private fun persist(response: AuthResponse) {
        require(response.accessToken.isNotBlank() && response.tokenType.equals("Bearer", true)) {
            "The authentication server returned an invalid session."
        }
        tokenStore.saveToken(response.accessToken)
    }
}
