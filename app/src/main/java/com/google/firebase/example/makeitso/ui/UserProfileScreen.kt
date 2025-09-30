package com.google.firebase.example.makeitso.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.EmailAuthProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    openSettingsScreen: () -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Профиль пользователя") },
                navigationIcon = {
                    IconButton(onClick = openSettingsScreen) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (currentUser == null) {
                Text(
                    "Пользователь не аутентифицирован",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                // Основная информация
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Основная информация",
                            style = MaterialTheme.typography.titleMedium
                        )

                        InfoRow(
                            icon = Icons.Default.Person,
                            title = "Имя",
                            value = currentUser.displayName ?: "Не указано"
                        )

                        InfoRow(
                            icon = Icons.Default.Email,
                            title = "Email",
                            value = currentUser.email ?: "Не указан"
                        )

                        InfoRow(
                            icon = Icons.Default.Check,
                            title = "Email подтвержден",
                            value = if (currentUser.isEmailVerified) "Да" else "Нет"
                        )
                    }
                }

                // Метод аутентификации
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Метод аутентификации",
                            style = MaterialTheme.typography.titleMedium
                        )

                        val authMethods = getAuthMethods(currentUser)
                        authMethods.forEach { method ->
                            Text(
                                "• $method",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Дополнительная информация
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Дополнительная информация",
                            style = MaterialTheme.typography.titleMedium
                        )

                        InfoRow(
                            title = "UID",
                            value = currentUser.uid
                        )

                        InfoRow(
                            title = "Дата создания",
                            value = currentUser.metadata?.creationTimestamp?.let {
                                formatTimestamp(it)
                            } ?: "Неизвестно"
                        )

                        InfoRow(
                            title = "Последний вход",
                            value = currentUser.metadata?.lastSignInTimestamp?.let {
                                formatTimestamp(it)
                            } ?: "Неизвестно"
                        )
                    }
                }

                // Провайдеры
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Подключенные провайдеры",
                            style = MaterialTheme.typography.titleMedium
                        )

                        currentUser.providerData.forEach { userInfo ->
                            UserInfoCard(userInfo)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    title: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun UserInfoCard(userInfo: com.google.firebase.auth.UserInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Провайдер: ${getProviderName(userInfo.providerId)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "UID: ${userInfo.uid}",
                style = MaterialTheme.typography.bodySmall
            )
            userInfo.displayName?.let { name ->
                Text(
                    "Имя: $name",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            userInfo.email?.let { email ->
                Text(
                    "Email: $email",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun getAuthMethods(user: com.google.firebase.auth.FirebaseUser): List<String> {
    val methods = mutableListOf<String>()

    user.providerData.forEach { userInfo ->
        when (userInfo.providerId) {
            GoogleAuthProvider.PROVIDER_ID -> methods.add("Google")
            EmailAuthProvider.PROVIDER_ID -> methods.add("Email/Пароль")
            "password" -> methods.add("Email/Пароль")
            else -> methods.add("Другой метод (${userInfo.providerId})")
        }
    }

    return methods.distinct()
}

private fun getProviderName(providerId: String): String {
    return when (providerId) {
        GoogleAuthProvider.PROVIDER_ID -> "Google"
        EmailAuthProvider.PROVIDER_ID -> "Email/Пароль"
        "password" -> "Email/Пароль"
        "firebase" -> "Firebase"
        else -> providerId
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val date = java.util.Date(timestamp)
        val formatter = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        "Неизвестно"
    }
}