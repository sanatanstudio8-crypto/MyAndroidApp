package com.example.myandroidapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myandroidapp.data.ApiServiceFactory
import com.example.myandroidapp.data.LoginRequest
import com.example.myandroidapp.data.SignupRequest
import com.example.myandroidapp.security.SecureTokenStore
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSignup by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (isSignup) "Create your account" else "Welcome back", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(18.dp))
        if (isSignup) {
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Full name") }, singleLine = true)
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            password, { password = it }, Modifier.fillMaxWidth(),
            label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank() || (isSignup && name.isBlank())) {
                    message = "Please complete all required fields."
                } else {
                    busy = true
                    message = null
                    scope.launch {
                        try {
                            val api = ApiServiceFactory.auth(context)
                            val response = if (isSignup) {
                                api.signup(SignupRequest(name.trim(), email.trim(), password))
                            } else {
                                api.login(LoginRequest(email.trim(), password))
                            }
                            SecureTokenStore(context).saveToken(response.accessToken)
                            onAuthenticated()
                        } catch (error: Exception) {
                            message = error.message?.take(180) ?: "Unable to connect to the account service."
                        } finally {
                            busy = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) {
            Text(if (busy) "Please wait…" else if (isSignup) "Sign up" else "Log in")
        }
        TextButton(onClick = { isSignup = !isSignup; message = null }) {
            Text(if (isSignup) "Already have an account? Log in" else "New here? Create an account")
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
