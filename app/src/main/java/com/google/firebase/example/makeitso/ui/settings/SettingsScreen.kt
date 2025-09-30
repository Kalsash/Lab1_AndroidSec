package com.google.firebase.example.makeitso.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.example.makeitso.R
import com.google.firebase.example.makeitso.ui.shared.CenterTopAppBar
import com.google.firebase.example.makeitso.ui.shared.StandardButton
import com.google.firebase.example.makeitso.ui.theme.DarkBlue
import com.google.firebase.example.makeitso.ui.theme.DarkGrey
import com.google.firebase.example.makeitso.ui.theme.LightRed
import com.google.firebase.example.makeitso.ui.theme.MakeItSoTheme
import kotlinx.serialization.Serializable

// Импорты для Google Sign In
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme

@Serializable
object SettingsRoute

@Composable
fun SettingsScreen(
    openHomeScreen: () -> Unit,
    openSignInScreen: () -> Unit,
    openUserProfileScreen: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    // Получаем clientId из ресурсов
    val webClientId = remember {
        try {
            val resourceId = context.resources.getIdentifier(
                "default_web_client_id", "string", context.packageName
            )
            if (resourceId != 0) {
                context.getString(resourceId)
            } else {
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
                            // Перезагружаем приложение после успешного входа
                            openHomeScreen()
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

    val shouldRestartApp by viewModel.shouldRestartApp.collectAsStateWithLifecycle()

    if (shouldRestartApp) {
        openHomeScreen()
    } else {
        SettingsScreenContent(
            loadCurrentUser = viewModel::loadCurrentUser,
            openSignInScreen = openSignInScreen,
            signOut = viewModel::signOut,
            deleteAccount = viewModel::deleteAccount,
            signInWithGoogle = {
                errorMessage = null
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            },
            currentUser = currentUser,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onSignOut = {
                FirebaseAuth.getInstance().signOut()
                googleSignInClient.signOut()
                currentUser = null
                errorMessage = null
            },
            openUserProfileScreen = openUserProfileScreen
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreenContent(
    loadCurrentUser: () -> Unit,
    openSignInScreen: () -> Unit,
    signOut: () -> Unit,
    deleteAccount: () -> Unit,
    signInWithGoogle: () -> Unit,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    isLoading: Boolean,
    errorMessage: String?,
    onSignOut: () -> Unit,
    openUserProfileScreen: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    LaunchedEffect(true) {
        loadCurrentUser()
    }

    Scaffold(
        topBar = {
            CenterTopAppBar(
                title = stringResource(R.string.settings),
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.size(24.dp))

            // Отображение информации о текущем пользователе
            currentUser?.let { user ->
                Text(
                    text = if (user.isAnonymous) {
                        "Анонимный пользователь"
                    } else {
                        "Привет, ${user.displayName ?: user.email ?: "Пользователь"}!"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Spacer(Modifier.size(16.dp))
            }

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

            if (currentUser == null || currentUser.isAnonymous) {
                // Для анонимных/неавторизованных пользователей показываем кнопки входа
                StandardButton(
                    label = R.string.sign_in,
                    onButtonClick = {
                        openSignInScreen()
                    }
                )

                Spacer(Modifier.size(16.dp))

                // Кнопка входа через Google
                StandardButton(
                    label = R.string.sign_in_with_google,
                    onButtonClick = {
                        signInWithGoogle()
                    }
                )
            } else {
                // Для авторизованных пользователей показываем кнопки выхода и профиль

                // Кнопка профиля пользователя
                StandardButton(
                    label = R.string.user_profile,
                    onButtonClick = {
                        openUserProfileScreen()
                    }
                )

                Spacer(Modifier.size(16.dp))

                StandardButton(
                    label = R.string.sign_out,
                    onButtonClick = {
                        onSignOut()
                        signOut()
                    }
                )

                Spacer(Modifier.size(16.dp))

                DeleteAccountButton(deleteAccount)
            }
        }
    }
}

@Composable
fun DeleteAccountButton(deleteAccount: () -> Unit) {
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    StandardButton(
        label = R.string.delete_account,
        onButtonClick = {
            showDeleteAccountDialog = true
        }
    )

    if (showDeleteAccountDialog) {
        AlertDialog(
            containerColor = LightRed,
            textContentColor = DarkBlue,
            titleContentColor = DarkBlue,
            title = { Text(stringResource(R.string.delete_account_title)) },
            text = { Text(stringResource(R.string.delete_account_description)) },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false },
                    colors = getDialogButtonColors()
                ) {
                    Text(text = stringResource(R.string.cancel), fontSize = 16.sp)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAccountDialog = false
                        deleteAccount()
                    },
                    colors = getDialogButtonColors()
                ) {
                    Text(text = stringResource(R.string.delete), fontSize = 16.sp)
                }
            },
            onDismissRequest = { showDeleteAccountDialog = false }
        )
    }
}

private fun getDialogButtonColors(): ButtonColors {
    return ButtonColors(
        containerColor = LightRed,
        contentColor = DarkBlue,
        disabledContainerColor = LightRed,
        disabledContentColor = DarkGrey
    )
}

@Composable
@Preview(showSystemUi = true)
fun SettingsScreenPreview() {
    MakeItSoTheme(darkTheme = true) {
        SettingsScreenContent(
            loadCurrentUser = {},
            openSignInScreen = {},
            signOut = {},
            deleteAccount = {},
            signInWithGoogle = {},
            currentUser = null,
            isLoading = false,
            errorMessage = null,
            onSignOut = {},
            openUserProfileScreen = {}
        )
    }
}