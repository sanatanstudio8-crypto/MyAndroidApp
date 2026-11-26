package com.example.myandroidapp

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.myandroidapp.data.ApiServiceFactory
import com.example.myandroidapp.security.SecureTokenStore
import kotlinx.coroutines.launch

@Composable
fun AuthenticationGate(content: @Composable (onLogout: () -> Unit) -> Unit) {
    val context = LocalContext.current
    val tokenStore = remember { SecureTokenStore(context.applicationContext) }
    var authenticated by remember { mutableStateOf(!tokenStore.readToken().isNullOrBlank()) }
    var signup by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (authenticated && !tokenStore.readToken().isNullOrBlank()) {
        content {
            tokenStore.clear()
            authenticated = false
        }
        return
    }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF191820)).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (signup) "Create your BALIDAN account" else "Sign in to BALIDAN",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text("Use your BALIDAN learning account.", color = Color(0xFFB8B4C7))
            Spacer(Modifier.height(20.dp))
            if (signup) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(Modifier.height(10.dp))
            }
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it }, label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = Color(0xFFFF8A80))
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    error = null
                    busy = true
                    scope.launch {
                        try {
                            val auth = ApiServiceFactory.authRepository(context)
                            if (signup) auth.signup(name, email, password) else auth.login(email, password)
                            authenticated = true
                        } catch (e: Exception) {
                            error = e.message?.take(160) ?: "Authentication failed. Please try again."
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && email.isNotBlank() && password.isNotBlank() &&
                    (!signup || name.trim().length >= 2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (busy) "Please wait…" else if (signup) "Create account" else "Sign in")
            }
            TextButton(
                onClick = { signup = !signup; error = null },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (signup) "Already registered? Sign in" else "New here? Create account")
            }
        }
    }
}
