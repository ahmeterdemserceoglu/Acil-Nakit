package com.acilnakit

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.acilnakit.ui.screens.*
import com.acilnakit.ui.theme.AcilNakitTheme
import com.acilnakit.ui.theme.getSchoolColor
import com.acilnakit.ui.viewmodel.AuthState
import com.acilnakit.ui.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    private var pendingDeepLink: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Android 13+ Bildirim İzni İste
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 101)
            }
        }

        // Handle initial deep link
        handleDeepLink(intent)
        
        setContent {
            val authViewModel: AuthViewModel = hiltViewModel()
            val schoolName by authViewModel.schoolName.collectAsState()
            val primaryColor = getSchoolColor(schoolName)

            AcilNakitTheme(primaryColor = primaryColor) {
                AcilNakitNavigation(
                    authViewModel = authViewModel,
                    initialDeepLink = pendingDeepLink,
                    onDeepLinkHandled = { pendingDeepLink = null }
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        val scheme = data.scheme
        val host = data.host
        
        android.util.Log.d("MainActivity", "Deep link received: $data")
        
        // acilnakit://payment-success?amount=100
        // acilnakit://payment-failed
        if (scheme == "acilnakit") {
            when (host) {
                "payment-success" -> {
                    val amount = data.getQueryParameter("amount")
                    pendingDeepLink = "payment-success"
                    Toast.makeText(this, "₺$amount başarıyla yüklendi! 🎉", Toast.LENGTH_LONG).show()
                }
                "payment-failed" -> {
                    pendingDeepLink = "payment-failed"
                    Toast.makeText(this, "Ödeme başarısız oldu. Lütfen tekrar deneyin.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@Composable
fun AcilNakitNavigation(
    authViewModel: AuthViewModel = hiltViewModel(),
    initialDeepLink: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    var deepLinkHandled by remember { mutableStateOf(false) }

    // Handle deep link navigation
    LaunchedEffect(initialDeepLink) {
        if (initialDeepLink != null && !deepLinkHandled) {
            when (initialDeepLink) {
                "payment-success", "payment-failed" -> {
                    // Navigate to wallet on payment callback
                    if (authState is AuthState.Verified) {
                        navController.navigate("wallet") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                }
            }
            deepLinkHandled = true
            onDeepLinkHandled()
        }
    }

    // Reactive current route tracking
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Global AuthState observer
    LaunchedEffect(authState, currentRoute) {
        android.util.Log.d("MainActivity", "AuthState: $authState, Current Route: $currentRoute")
        
        when (authState) {
            is AuthState.Unauthenticated -> {
                if (currentRoute != "login" && currentRoute != null) {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.NeedsCampus -> {
                if (currentRoute != "campus_selection" && currentRoute != null) {
                    navController.navigate("campus_selection") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.Verified -> {
                if (currentRoute == "login" || currentRoute == "splash") {
                    navController.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.NotVerifiedYet -> {
                if (currentRoute != "login" && currentRoute != null) {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.Suspended -> {
                if (currentRoute != "suspended") {
                    navController.navigate("suspended") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }

    
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                viewModel = authViewModel // Pass shared VM
            )
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    // Logic handled by observer
                },
                viewModel = authViewModel // Pass shared VM
            )
        }
        composable("campus_selection") {
            CampusSelectionScreen(
                onProfileComplete = {
                    navController.navigate("home") {
                        popUpTo("campus_selection") { inclusive = true }
                    }
                },
                viewModel = authViewModel // Pass shared VM
            )
        }
        composable("home") {
            HomeScreen(
                onCreateTask = { navController.navigate("create_task") },
                onTaskClick = { taskId -> navController.navigate("task_detail/$taskId") },
                onProfileClick = { navController.navigate("profile") },
                onWalletClick = { navController.navigate("wallet") },
                onActiveTasksClick = { navController.navigate("active_tasks") },
                onNotificationsClick = { navController.navigate("notifications") }
            )
        }
        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate("history") },
                authViewModel = authViewModel
            )
        }
        composable("wallet") {
            WalletScreen(
                onBack = { navController.popBackStack() },
                onNavigateToWithdraw = { balance -> navController.navigate("withdrawal/$balance") },
                onNavigateToTopUp = { navController.navigate("topup") }
            )
        }
        composable("withdrawal/{balance}") { backStackEntry ->
            val balance = backStackEntry.arguments?.getString("balance")?.toDoubleOrNull() ?: 0.0
            WithdrawalScreen(
                onBack = { navController.popBackStack() },
                currentBalance = balance
            )
        }
        composable("history") {
            TaskHistoryScreen(
                onBack = { navController.popBackStack() },
                onTaskClick = { taskId -> navController.navigate("task_detail/$taskId") }
            )
        }
        composable("topup") {
            TopUpScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("create_task") {
            CreateTaskScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { 
                    navController.popBackStack()
                },
                onNavigateToWallet = { navController.navigate("wallet") }
            )
        }
        composable("task_detail/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            TaskDetailScreen(
                taskId = taskId,
                onBack = { navController.popBackStack() },
                onContact = { chatId -> navController.navigate("chat/$chatId") },
                onViewProfile = { userId -> navController.navigate("user_profile/$userId") },
                onEditTask = { navController.navigate("edit_task/$taskId") }
            )
        }
        composable("chat/{chatId}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() }
            )
        }
        
        // Yeni Ekranlar
        composable("active_tasks") {
            ActiveTasksScreen(
                onBack = { navController.popBackStack() },
                onTaskClick = { taskId -> navController.navigate("task_detail/$taskId") }
            )
        }
        composable("user_profile/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserProfileScreen(
                userId = userId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("edit_task/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            EditTaskScreen(
                taskId = taskId,
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }
        composable("notifications") {
            NotificationScreen(
                onBack = { navController.popBackStack() },
                onTaskClick = { taskId -> navController.navigate("task_detail/$taskId") },
                onChatClick = { chatId -> navController.navigate("chat/$chatId") }
            )
        }
        composable("suspended") {
            SuspendedScreen()
        }
    }
}