package com.example.sns_v1

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.animation.Crossfade
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.example.sns_v1.network.ApiClient
import com.example.sns_v1.navigation.GrowLogNavGraph
import com.example.sns_v1.AppState
import com.example.sns_v1.auth.TokenManager
import com.example.sns_v1.navigation.Route
import com.example.sns_v1.navigation.currentRoute
import com.example.sns_v1.ui.components.GrowLogBottomBar
import com.example.sns_v1.ui.theme.Accent
import com.example.sns_v1.ui.theme.SNSV1Theme
import com.example.sns_v1.ui.screens.LaunchScreen
import com.example.sns_v1.ui.screens.TutorialScreen
import com.example.sns_v1.ui.screens.AccountCreationScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : ComponentActivity() {

    private data class ProfileDraft(val displayName: String, val biography: String)

    private lateinit var auth: FirebaseAuth
    private var isLoggedIn by mutableStateOf(false)
    private var sessionVerified by mutableStateOf(false)
    private var requiresTutorial by mutableStateOf(false)
    private var needsProfileSetup by mutableStateOf(false)
    private var requiresProfileAfterLogin by mutableStateOf(false)
    private var pendingProfile: ProfileDraft? = null

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("GrowLog", "Sign-in result code: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                } else {
                    Log.e("GrowLog", "idToken is null")
                    Toast.makeText(this, "ログインに失敗しました（トークン取得エラー）", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                Log.e("GrowLog", "Google sign in failed. StatusCode: ${e.statusCode}", e)
                val msg = when (e.statusCode) {
                    12500 -> "ログイン失敗: SHA-1フィンガープリントがFirebaseに未登録です"
                    12501 -> "ログインがキャンセルされました"
                    else  -> "ログインエラー: ${e.statusCode}"
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        } else {
            Log.w("GrowLog", "Sign-in not OK, resultCode=${result.resultCode}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()
        val currentFirebaseUser = auth.currentUser
        isLoggedIn = currentFirebaseUser != null

        if (currentFirebaseUser == null) {
            sessionVerified = true
        } else {
            // Firebase に問い合わせ、削除・無効化されたアカウントを起動時に検出する。
            currentFirebaseUser.reload().addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("GrowLog", "Firebase account is no longer valid", task.exception)
                    signOut()
                    requiresTutorial = true
                    sessionVerified = true
                    return@addOnCompleteListener
                }
                lifecycleScope.launch {
                    try {
                        val idToken = TokenManager.getIdToken()
                        ApiClient.instance.login(idToken).onSuccess { user ->
                            AppState.userId = user.userId
                            AppState.userName = user.userName
                            AppState.displayName = user.displayName
                            Log.d("GrowLog", "Session restored: ${user.userName}")
                        }
                    } catch (e: Exception) {
                        Log.w("GrowLog", "Session restore failed", e)
                    } finally {
                        sessionVerified = true
                    }
                }
            }
        }

        setContent {
            SNSV1Theme {
                val navController = rememberNavController()
                val currentRoute = currentRoute(navController)
                // 起動演出は復元せず、Activity が新しく起動するたびに必ず表示する。
                var showLaunchScreen by remember { mutableStateOf(true) }
                val onboardingPreferences = remember {
                    getSharedPreferences("growlog_onboarding", MODE_PRIVATE)
                }
                var showTutorial by remember {
                    // 未ログインなら、アプリを起動するたびチュートリアルから始める。
                    mutableStateOf(!isLoggedIn)
                }
                var showAccountCreation by remember { mutableStateOf(false) }
                val mainRoutes = setOf(
                    Route.Home.route, Route.Discover.route,
                    Route.Notifications.route, Route.Messages.route, Route.Profile.route
                )
                val showBottomBar = currentRoute in mainRoutes

                LaunchedEffect(isLoggedIn) {
                    // 起動時に既にログイン済みなら開始画面がそのまま Home なので、
                    // ここで navigate すると Home がバックスタックに二重に積まれる
                    if (isLoggedIn && currentRoute == Route.Login.route) {
                        navController.navigate(Route.Home.route) {
                            popUpTo(Route.Login.route) { inclusive = true }
                        }
                    }
                }

                LaunchedEffect(requiresTutorial) {
                    if (requiresTutorial) {
                        showTutorial = true
                        requiresTutorial = false
                    }
                }

                Crossfade(targetState = showLaunchScreen || !sessionVerified, label = "launchTransition") { showingLaunch ->
                    if (showingLaunch) {
                        LaunchScreen(onFinished = { showLaunchScreen = false })
                    } else if (showTutorial) {
                        TutorialScreen(
                            onFinished = {
                                onboardingPreferences.edit().putBoolean("completed", true).apply()
                                showTutorial = false
                                showAccountCreation = true
                            },
                            onLogin = {
                                onboardingPreferences.edit().putBoolean("completed", true).apply()
                                showTutorial = false
                            }
                        )
                    } else if (showAccountCreation || requiresProfileAfterLogin) {
                        AccountCreationScreen(
                            onComplete = { displayName, biography ->
                                if (requiresProfileAfterLogin) {
                                    completeProfileSetup(displayName, biography)
                                } else {
                                    showAccountCreation = false
                                    startGoogleSignIn(ProfileDraft(displayName, biography))
                                }
                            },
                            onCancel = { showAccountCreation = false },
                            canExit = !requiresProfileAfterLogin
                        )
                    } else {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            // 透明にすると端末のウィンドウ背景（白）が上下の帯に出てしまう
                            containerColor = MaterialTheme.colorScheme.background,
                            // X と同じく、投稿はメニューではなく右下の丸ボタンから
                            floatingActionButton = {
                                if (showBottomBar) {
                                    FloatingActionButton(
                                        onClick = { navController.navigate(Route.CreatePost.route) },
                                        containerColor = Accent,
                                        contentColor = Color.White,
                                        shape = CircleShape,
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Add,
                                            contentDescription = "投稿",
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            },
                            bottomBar = {
                                if (showBottomBar) {
                                    GrowLogBottomBar(
                                        currentRoute = currentRoute,
                                        onNavigate = { route ->
                                            navController.navigate(route) {
                                                popUpTo(Route.Home.route) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            GrowLogNavGraph(
                                navController = navController,
                                isLoggedIn = isLoggedIn,
                                onGoogleSignIn = { startGoogleSignIn() },
                                onLogout = {
                                    signOut()
                                    onboardingPreferences.edit().remove("completed").apply()
                                    showTutorial = true
                                    navController.navigate(Route.Login.route) {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    /** Firebase と Google の両方からサインアウトしないと、次回のログインで
     *  アカウント選択が出ずに同じアカウントへ即再ログインしてしまう */
    private fun signOut() {
        auth.signOut()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this, gso).signOut()

        AppState.userId = ""
        AppState.userName = ""
        AppState.displayName = ""
        isLoggedIn = false
    }

    private fun startGoogleSignIn(profile: ProfileDraft? = null) {
        pendingProfile = profile
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun completeProfileSetup(displayName: String, biography: String) {
        lifecycleScope.launch {
            try {
                ApiClient.instance.updateProfile(
                    idToken = TokenManager.getIdToken(),
                    displayName = displayName,
                    biography = biography
                ).onSuccess {
                    AppState.displayName = displayName
                    needsProfileSetup = false
                    requiresProfileAfterLogin = false
                    // 登録画面を閉じると、ログイン済みのナビゲーションがホームを表示する。
                }.onFailure { error ->
                    // 接続先の旧 API に PATCH ルートがない場合でも、初回導線を止めない。
                    if (error.message?.contains("404") == true) {
                        AppState.displayName = displayName
                        needsProfileSetup = false
                        requiresProfileAfterLogin = false
                        Toast.makeText(this@MainActivity, "プロフィールは後から編集できます", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, error.message ?: "プロフィールを保存できませんでした", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, e.message ?: "プロフィールを保存できませんでした", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener(this) { authResult ->
                // プロフィールが入力済みの場合だけ、認証後に新規アカウントへ保存する。
                val firebaseNewUser = authResult.additionalUserInfo?.isNewUser == true
                Log.d("GrowLog", "Firebase auth success: ${auth.currentUser?.email}")
                lifecycleScope.launch {
                    // Workers は Google の OAuth トークンではなく Firebase ID トークンを検証する。
                    val firebaseIdToken = TokenManager.getIdToken()
                    val result = ApiClient.instance.login(firebaseIdToken)
                    result.onSuccess { user ->
                        Log.d("GrowLog", "API login: userName=${user.userName}, isNew=${user.isNewUser}")
                        AppState.userId = user.userId
                        AppState.userName = user.userName
                        AppState.displayName = user.displayName
                        val draft = pendingProfile
                        if ((user.isNewUser || firebaseNewUser) && draft != null) {
                            ApiClient.instance.updateProfile(
                                idToken = firebaseIdToken,
                                displayName = draft.displayName,
                                biography = draft.biography
                            )
                            AppState.displayName = draft.displayName
                        }
                        if ((user.isNewUser || firebaseNewUser) && draft == null) {
                            requiresProfileAfterLogin = true
                        }
                        pendingProfile = null
                    }.onFailure { e ->
                        Log.w("GrowLog", "API login failed (continuing with local auth)", e)
                        if (firebaseNewUser && pendingProfile == null) {
                            requiresProfileAfterLogin = true
                        }
                    }
                    isLoggedIn = true
                }
            }
            .addOnFailureListener(this) { e ->
                Log.e("GrowLog", "Firebase auth failed", e)
                Toast.makeText(this, "Firebase認証エラー: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
