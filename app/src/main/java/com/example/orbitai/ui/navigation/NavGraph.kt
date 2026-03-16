package com.example.orbitai.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.orbitai.ui.screens.ChatScreen
import com.example.orbitai.ui.screens.HomeScreen
import com.example.orbitai.ui.screens.MemoryScreen
import com.example.orbitai.ui.screens.SpaceDetailScreen
import com.example.orbitai.ui.screens.SpacesScreen
import com.example.orbitai.ui.screens.SettingsScreen
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.ChatViewModel
import com.example.orbitai.viewmodel.DownloadViewModel
import com.example.orbitai.viewmodel.MemoryViewModel
import com.example.orbitai.viewmodel.SpacesViewModel

sealed class Screen(val route: String) {
    data object Home     : Screen("home")
    data object Spaces   : Screen("spaces")
    data object Memory   : Screen("memory")
    data object Settings : Screen("settings")
    data object Chat     : Screen("chat/{chatId}") {
        fun go(chatId: String) = "chat/$chatId"
    }
    data object SpaceDetail : Screen("space_detail/{spaceId}") {
        fun go(spaceId: String) = "space_detail/$spaceId"
    }
}

private val TAB_ROUTES = setOf(
    Screen.Home.route,
    Screen.Spaces.route,
    Screen.Memory.route,
    Screen.Settings.route,
)

@Composable
fun OrbitNavGraph(
    navController: NavHostController,
    chatViewModel: ChatViewModel,
    downloadViewModel: DownloadViewModel,
    spacesViewModel: SpacesViewModel,
    memoryViewModel: MemoryViewModel,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route
    val showBottomBar  = currentRoute in TAB_ROUTES

    Scaffold(
        containerColor = Void,
        bottomBar = {
            if (showBottomBar) {
                OrbitBottomBar(
                    currentRoute = currentRoute,
                    onNavigate   = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    onNewChat = {
                        val id = chatViewModel.createNewChat()
                        navController.navigate(Screen.Chat.go(id))
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController     = navController,
            startDestination  = Screen.Home.route,
            modifier          = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel  = chatViewModel,
                    onOpenChat = { navController.navigate(Screen.Chat.go(it)) },
                )
            }

            composable(Screen.Spaces.route) {
                SpacesScreen(
                    viewModel    = spacesViewModel,
                    onOpenSpace  = { navController.navigate(Screen.SpaceDetail.go(it)) },
                )
            }

            composable(Screen.Memory.route) {
                MemoryScreen(viewModel = memoryViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    downloadViewModel = downloadViewModel,
                    onBack            = { navController.popBackStack() },
                )
            }

            composable(
                route     = Screen.Chat.route,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { back ->
                val chatId = back.arguments?.getString("chatId") ?: return@composable
                ChatScreen(
                    chatId    = chatId,
                    viewModel = chatViewModel,
                    onBack    = { navController.popBackStack() },
                )
            }

            composable(
                route     = Screen.SpaceDetail.route,
                arguments = listOf(navArgument("spaceId") { type = NavType.StringType })
            ) { back ->
                val spaceId = back.arguments?.getString("spaceId") ?: return@composable
                SpaceDetailScreen(
                    spaceId   = spaceId,
                    viewModel = spacesViewModel,
                    onBack    = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun OrbitBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onNewChat: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Surface(
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(22.dp),
            color         = Surface1,
            shadowElevation = 12.dp,
            tonalElevation  = 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                NavBarItem(
                    icon     = Icons.Default.ChatBubbleOutline,
                    label    = "Chats",
                    selected = currentRoute == Screen.Home.route,
                    onClick  = { onNavigate(Screen.Home.route) },
                )

                NavBarItem(
                    icon     = Icons.Default.Folder,
                    label    = "Spaces",
                    selected = currentRoute == Screen.Spaces.route,
                    onClick  = { onNavigate(Screen.Spaces.route) },
                )

                // Centre + button
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            Brush.linearGradient(listOf(CyanCore, CyanCore.copy(0.75f)))
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null,
                            onClick           = onNewChat,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint     = Void,
                        modifier = Modifier.size(26.dp),
                    )
                }

                NavBarItem(
                    icon     = Icons.Default.Memory,
                    label    = "Memory",
                    selected = currentRoute == Screen.Memory.route,
                    onClick  = { onNavigate(Screen.Memory.route) },
                )

                NavBarItem(
                    icon     = Icons.Default.Settings,
                    label    = "Settings",
                    selected = currentRoute == Screen.Settings.route,
                    onClick  = { onNavigate(Screen.Settings.route) },
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue   = if (selected) 1.1f else 1f,
        animationSpec = tween(150),
        label         = "scale",
    )
    val iconTint  = if (selected) CyanCore else TextMuted
    val labelColor = if (selected) CyanCore else TextMuted

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = label, tint = iconTint, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 10.sp, color = labelColor, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
