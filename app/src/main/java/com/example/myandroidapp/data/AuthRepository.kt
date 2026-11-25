package com.example.myandroidapp.data

class AuthRepository(private val api: AuthApi) {
    suspend fun login(email: String, password: String) = api.login(LoginRequest(email, password))
    suspend fun signup(name: String, email: String, password: String) = api.signup(SignupRequest(name, email, password))
}
