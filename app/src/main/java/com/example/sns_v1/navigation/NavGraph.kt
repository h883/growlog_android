package com.example.sns_v1.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.sns_v1.ui.screens.CreatePostScreen
import com.example.sns_v1.ui.screens.DiscoverScreen
import com.example.sns_v1.ui.screens.GroupDetailScreen
import com.example.sns_v1.ui.screens.GroupsScreen
import com.example.sns_v1.ui.screens.EditProfileScreen
import com.example.sns_v1.ui.screens.FocusScreen
import com.example.sns_v1.ui.screens.HomeScreen
import com.example.sns_v1.ui.screens.ChatScreen
import com.example.sns_v1.ui.screens.LoginScreen
import com.example.sns_v1.ui.screens.MessagesScreen
import com.example.sns_v1.ui.screens.NotificationsScreen
import com.example.sns_v1.ui.screens.PostDetailScreen
import com.example.sns_v1.ui.screens.ProfileScreen
import com.example.sns_v1.ui.screens.SettingsScreen
import com.example.sns_v1.ui.screens.UserProfileScreen
import com.example.sns_v1.ui.screens.GoalListScreen
import com.example.sns_v1.ui.screens.GoalDetailScreen
import com.example.sns_v1.viewmodel.ChatViewModel
import com.example.sns_v1.viewmodel.CreatePostViewModel
import com.example.sns_v1.viewmodel.MessagesViewModel
import com.example.sns_v1.viewmodel.DiscoverViewModel
import com.example.sns_v1.viewmodel.FocusViewModel
import com.example.sns_v1.viewmodel.GroupDetailViewModel
import com.example.sns_v1.viewmodel.GroupsViewModel
import com.example.sns_v1.viewmodel.NotificationsViewModel
import com.example.sns_v1.viewmodel.PostDetailViewModel
import com.example.sns_v1.viewmodel.PostsViewModel
import com.example.sns_v1.viewmodel.ProfileViewModel
import com.example.sns_v1.viewmodel.UserProfileViewModel

@Composable
fun GrowLogNavGraph(
    navController: NavHostController,
    isLoggedIn: Boolean,
    onGoogleSignIn: () -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val startDestination = if (isLoggedIn) Route.Home.route else Route.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Route.Login.route) {
            LoginScreen(onGoogleSignIn = onGoogleSignIn)
        }
        composable(Route.Home.route) {
            val viewModel: PostsViewModel = viewModel()
            HomeScreen(
                viewModel = viewModel,
                onNavigateToNotifications = { navController.navigate(Route.Notifications.route) },
                onNavigateToPostDetail = { postId -> navController.navigate(Route.PostDetail.createRoute(postId)) },
                onNavigateToUserProfile = { userName -> navController.navigate(Route.UserProfile.createRoute(userName)) },
                onNavigateToDiscover = { navController.navigate(Route.Discover.route) },
                onNavigateToMessages = { navController.navigate(Route.Messages.route) },
                onNavigateToFocus = { navController.navigate(Route.Focus.route) }
            )
        }
        composable(Route.Messages.route) {
            val viewModel: MessagesViewModel = viewModel()
            MessagesScreen(
                viewModel = viewModel,
                onOpenChat = { id -> navController.navigate(Route.Chat.createRoute(id)) },
                showBack = false
            )
        }
        composable(
            route = Route.Chat.route,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val viewModel: ChatViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return ChatViewModel(conversationId) as T
                }
            })
            ChatScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToUserProfile = { userName ->
                    navController.navigate(Route.UserProfile.createRoute(userName))
                }
            )
        }
        composable(Route.Focus.route) {
            val viewModel: FocusViewModel = viewModel()
            FocusScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToUserProfile = { userName -> navController.navigate(Route.UserProfile.createRoute(userName)) }
            )
        }
        composable(Route.Groups.route) {
            val viewModel: GroupsViewModel = viewModel()
            GroupsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenGroup = { id -> navController.navigate(Route.GroupDetail.createRoute(id)) }
            )
        }
        composable(
            route = Route.GroupDetail.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val viewModel: GroupDetailViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return GroupDetailViewModel(groupId) as T
                }
            })
            GroupDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToPostDetail = { postId -> navController.navigate(Route.PostDetail.createRoute(postId)) },
                onNavigateToUserProfile = { userName -> navController.navigate(Route.UserProfile.createRoute(userName)) }
            )
        }
        composable(Route.Discover.route) {
            val viewModel: DiscoverViewModel = viewModel()
            DiscoverScreen(
                viewModel = viewModel,
                onNavigateToPostDetail = { postId -> navController.navigate(Route.PostDetail.createRoute(postId)) },
                onNavigateToUserProfile = { userName -> navController.navigate(Route.UserProfile.createRoute(userName)) },
                onNavigateToGroups = { navController.navigate(Route.Groups.route) }
            )
        }
        composable(Route.CreatePost.route) {
            val viewModel: CreatePostViewModel = viewModel()
            CreatePostScreen(
                viewModel = viewModel,
                onCancel = { navController.popBackStack() },
                onPost = { navController.popBackStack() }
            )
        }
        composable(Route.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = onLogout,
                onEditProfile = { navController.navigate(Route.EditProfile.route) }
            )
        }
        composable(Route.Notifications.route) {
            val viewModel: NotificationsViewModel = viewModel()
            NotificationsScreen(viewModel = viewModel)
        }
        composable(Route.Profile.route) {
            val viewModel: ProfileViewModel = viewModel()
            ProfileScreen(
                viewModel = viewModel,
                onNavigateToPostDetail = { postId -> navController.navigate(Route.PostDetail.createRoute(postId)) },
                onNavigateToUserProfile = { userName -> navController.navigate(Route.UserProfile.createRoute(userName)) },
                onNavigateToEditProfile = { navController.navigate(Route.EditProfile.route) },
                onNavigateToSettings = { navController.navigate(Route.Settings.route) },
                onNavigateToDiscover = { navController.navigate(Route.Discover.route) },
                onNavigateToGoals = { navController.navigate(Route.Goals.route) }
            )
        }
        composable(Route.Goals.route) { backStackEntry ->
            val profileEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.Profile.route) }
            val viewModel: ProfileViewModel = viewModel(viewModelStoreOwner = profileEntry)
            GoalListScreen(viewModel, onBack = { navController.popBackStack() }, onOpenGoal = { id -> navController.navigate(Route.GoalDetail.createRoute(id)) })
        }
        composable(
            route = Route.GoalDetail.route,
            arguments = listOf(navArgument("goalId") { type = NavType.StringType })
        ) { backStackEntry ->
            val profileEntry = remember(backStackEntry) { navController.getBackStackEntry(Route.Profile.route) }
            val viewModel: ProfileViewModel = viewModel(viewModelStoreOwner = profileEntry)
            GoalDetailScreen(viewModel, backStackEntry.arguments?.getString("goalId") ?: "", onBack = { navController.popBackStack() })
        }
        composable(Route.EditProfile.route) { backStackEntry ->
            // 編集結果をプロフィール画面へそのまま反映したいので ViewModel を共有する
            val profileEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Route.Profile.route)
            }
            val viewModel: ProfileViewModel = viewModel(viewModelStoreOwner = profileEntry)
            EditProfileScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(
            route = Route.PostDetail.route,
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            val viewModel: PostDetailViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return PostDetailViewModel(postId) as T
                }
            })
            PostDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToUserProfile = { userName -> navController.navigate(Route.UserProfile.createRoute(userName)) }
            )
        }
        composable(
            route = Route.UserProfile.route,
            arguments = listOf(navArgument("userName") { type = NavType.StringType })
        ) { backStackEntry ->
            val userName = backStackEntry.arguments?.getString("userName") ?: ""
            val viewModel: UserProfileViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return UserProfileViewModel(userName) as T
                }
            })
            UserProfileScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToPostDetail = { postId -> navController.navigate(Route.PostDetail.createRoute(postId)) },
                onOpenChat = { id -> navController.navigate(Route.Chat.createRoute(id)) }
            )
        }
    }
}

@Composable
fun currentRoute(navController: NavHostController): String? {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return navBackStackEntry?.destination?.route
}
