package com.google.firebase.example.makeitso.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun GoogleSignInScreen(
    openTodoListScreen: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    // Получаем clientId из ресурсов или используем дефолтный
    val webClientId = remember {
        // Способ 1: Через строковый ресурс (рекомендуется)
        try {
            val resourceId = context.resources.getIdentifier(
                "default_web_client_id", "string", context.packageName
            )
            if (resourceId != 0) {
                context.getString(resourceId)
            } else {
                // Способ 2: Прямое указание (замените на ваш реальный ID)
                "ваш_web_client_id_здесь.apps.googleusercontent.com"
            }
        } catch (e: Exception) {
            "ваш_web_client_id_здесь.apps.googleusercontent.com"
        }
    }

    // Настройка Google Sign-In
    val googleSignInClient = remember {
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
        )
    }

    // Launcher для Google Sign-In
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLoading = true
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)

            if (account != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        isLoading = false
                        if (authTask.isSuccessful) {
                            currentUser = FirebaseAuth.getInstance().currentUser
                            errorMessage = null
                            // Автоматически переходим к списку дел после успешного входа
                            openTodoListScreen()
                        } else {
                            errorMessage = authTask.exception?.message ?: "Ошибка аутентификации"
                        }
                    }
            } else {
                isLoading = false
                errorMessage = "Не удалось получить данные аккаунта"
            }
        } catch (e: ApiException) {
            isLoading = false
            errorMessage = when (e.statusCode) {
                10 -> "Ошибка конфигурации. Проверьте:\n• Web Client ID\n• SHA-1 fingerprint\n• Подключение Google Sign-in в Firebase"
                12501 -> "Вход отменен пользователем"
                12502 -> "Вход отменен пользователем"
                4 -> "Сетевая ошибка. Проверьте интернет-соединение"
                7 -> "Ошибка подключения к сети"
                else -> "Ошибка входа через Google: ${e.statusCode} - ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (currentUser != null) {
                "Привет, ${currentUser?.displayName ?: currentUser?.email ?: "Пользователь"}!"
            } else {
                "Привет!"
            },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 32.dp)
        )

        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Выполняется вход...")
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (currentUser == null) {
            Button(
                onClick = {
                    errorMessage = null
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                }
            ) {
                Text("Войти через Google")
            }
        } else {
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    googleSignInClient.signOut()
                    currentUser = null
                    errorMessage = null
                }
            ) {
                Text("Выйти")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = openTodoListScreen,
            enabled = currentUser != null
        ) {
            Text("Перейти к списку дел")
        }
    }
}