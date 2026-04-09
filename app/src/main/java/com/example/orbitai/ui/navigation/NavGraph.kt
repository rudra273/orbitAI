package com.example.orbitai.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.orbitai.R
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.orbitai.OverlayPromptRequest
import com.example.orbitai.data.MemoryFeatureStore
import com.example.orbitai.data.ToolSettingsStore
import com.example.orbitai.data.TokenStore
import com.example.orbitai.ui.screens.ModesScreen
import com.example.orbitai.ui.screens.ChatScreen
import com.example.orbitai.ui.screens.ModelSettingsScreen
import com.example.orbitai.ui.screens.HomeScreen
import com.example.orbitai.ui.screens.MemorySettingsScreen
import com.example.orbitai.ui.screens.SpaceDetailScreen
import com.example.orbitai.ui.screens.SpacesScreen
import com.example.orbitai.ui.screens.SettingsScreen
import com.example.orbitai.ui.screens.OrbitBubbleSettingsScreen
import com.example.orbitai.ui.screens.ToolsSettingsScreen
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.ModesViewModel
import com.example.orbitai.viewmodel.AppUpdateViewModel
import com.example.orbitai.viewmodel.ChatViewModel
import com.example.orbitai.viewmodel.DownloadViewModel
import com.example.orbitai.viewmodel.MemoryViewModel
import com.example.orbitai.viewmodel.SpacesViewModel
import com.example.orbitai.tools.bubble.OrbitBubbleService

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ROUTE DEFINITIONS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

sealed class Screen(val route: String) {
    // ── 4 primary tabs ──────────────────────────────────────────────────────
    data object Chat     : Screen("chat_list")   // renamed from Home for clarity
    data object Spaces   : Screen("spaces")
    data object Modes    : Screen("modes")
    data object Settings : Screen("settings")

    // ── Detail screens (not in bottom nav) ──────────────────────────────────
    data object ChatDetail : Screen("chat/{chatId}") {
        fun go(chatId: String) = "chat/$chatId"
    }
    data object SpaceDetail : Screen("space_detail/{spaceId}") {
        fun go(spaceId: String) = "space_detail/$spaceId"
    }

    // ── Settings sub-screens ─────────────────────────────────────────────────
    data object SettingsModel     : Screen("settings/model")
    data object SettingsMemory    : Screen("settings/memory")      // moved from tab
    data object SettingsTools     : Screen("settings/tools")
    data object SettingsOrbitBubble : Screen("settings/orbit_bubble")
}

private val TAB_ROUTES = setOf(
    Screen.Chat.route,
    Screen.Spaces.route,
    Screen.Modes.route,
    Screen.Settings.route,
)

private val SWIPE_NAV_ROUTES = listOf(
    Screen.Chat.route,
    Screen.Spaces.route,
    Screen.Modes.route,
    Screen.Settings.route,
)

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ROOT NAV GRAPH
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
fun OrbitNavGraph(
    navController:     NavHostController,
    chatViewModel:     ChatViewModel,
    downloadViewModel: DownloadViewModel,
    appUpdateViewModel: AppUpdateViewModel,
    spacesViewModel:   SpacesViewModel,
    modesViewModel:    ModesViewModel,
    memoryViewModel:   MemoryViewModel,
    overlayPromptRequest: OverlayPromptRequest?,
    onOverlayPromptConsumed: () -> Unit,
    isDarkTheme:       Boolean,
    onThemeChanged:    (Boolean) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route
    val showBottomBar  = currentRoute in TAB_ROUTES
    val initialChatId = rememberSaveable { overlayPromptRequest?.chatId ?: chatViewModel.createNewChat() }
    val swipeModifier = rememberTabSwipeModifier(
        currentRoute = currentRoute,
        onNavigate = { route ->
            navController.navigate(route) {
                popUpTo(Screen.Chat.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
    )

    LaunchedEffect(initialChatId) {
        navController.navigate(Screen.ChatDetail.go(initialChatId)) {
            launchSingleTop = true
        }
    }

    LaunchedEffect(overlayPromptRequest?.id) {
        val request = overlayPromptRequest ?: return@LaunchedEffect
        navController.navigate(Screen.ChatDetail.go(request.chatId)) {
            launchSingleTop = true
        }
        chatViewModel.sendMessage(request.chatId, request.prompt)
        onOverlayPromptConsumed()
    }

    Scaffold(
        containerColor = SpaceDeep,
        bottomBar = {
            if (showBottomBar) {
                OrbitBottomBar(
                    currentRoute = currentRoute,
                    onNavigate   = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Chat.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceDeep),
        ) {
            NavHost(
                navController    = navController,
                startDestination = Screen.Chat.route,
                modifier         = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .then(swipeModifier),
            ) {

            // ── Tab screens ────────────────────────────────────────────────

            composable(Screen.Chat.route) {
                HomeScreen(
                    viewModel  = chatViewModel,
                    onOpenChat = { navController.navigate(Screen.ChatDetail.go(it)) },
                )
            }

            composable(Screen.Spaces.route) {
                SpacesScreen(
                    viewModel   = spacesViewModel,
                    onOpenSpace = { navController.navigate(Screen.SpaceDetail.go(it)) },
                )
            }

            composable(Screen.Modes.route) {
                ModesScreen(viewModel = modesViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    downloadViewModel = downloadViewModel,
                    appUpdateViewModel = appUpdateViewModel,
                    isDarkTheme       = isDarkTheme,
                    onThemeChanged    = onThemeChanged,
                    onNavigate        = { navController.navigate(it) },
                    onBack            = { navController.popBackStack() },
                )
            }

            // ── Detail screens ─────────────────────────────────────────────

            composable(
                route     = Screen.ChatDetail.route,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
            ) { back ->
                val chatId = back.arguments?.getString("chatId") ?: return@composable
                ChatScreen(
                    chatId    = chatId,
                    viewModel = chatViewModel,
                    onBack    = {
                        navController.navigate(Screen.Chat.route) {
                            popUpTo(Screen.ChatDetail.go(chatId)) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(
                route     = Screen.SpaceDetail.route,
                arguments = listOf(navArgument("spaceId") { type = NavType.StringType }),
            ) { back ->
                val spaceId = back.arguments?.getString("spaceId") ?: return@composable
                SpaceDetailScreen(
                    spaceId   = spaceId,
                    viewModel = spacesViewModel,
                    onBack    = { navController.popBackStack() },
                )
            }

            composable(Screen.SettingsModel.route) {
                val context = LocalContext.current
                val tokenStore = remember { TokenStore(context) }
                ModelSettingsScreen(
                    downloadViewModel = downloadViewModel,
                    tokenStore = tokenStore,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.SettingsMemory.route) {
                val context = LocalContext.current
                val memoryStore = remember { MemoryFeatureStore(context) }
                MemorySettingsScreen(
                    memoryViewModel = memoryViewModel,
                    memoryStore = memoryStore,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.SettingsTools.route) {
                val context = LocalContext.current
                val toolSettingsStore = remember { ToolSettingsStore(context) }
                ToolsSettingsScreen(
                    toolSettingsStore = toolSettingsStore,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.SettingsOrbitBubble.route) {
                val context = LocalContext.current
                val toolSettingsStore = remember { ToolSettingsStore(context) }
                val availableModels by chatViewModel.availableModels.collectAsState()
                val modes by chatViewModel.modes.collectAsState()
                LaunchedEffect(Unit) { chatViewModel.refreshAvailableModels() }
                OrbitBubbleSettingsScreen(
                    toolSettingsStore = toolSettingsStore,
                    availableModels = availableModels,
                    modes = modes,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        } // Box
    }
}

@Composable
private fun rememberTabSwipeModifier(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
): Modifier {
    if (currentRoute !in SWIPE_NAV_ROUTES) return Modifier

    return Modifier.pointerInput(currentRoute) {
        var totalDrag = 0f
        detectHorizontalDragGestures(
            onHorizontalDrag = { _, dragAmount ->
                totalDrag += dragAmount
            },
            onDragEnd = {
                val threshold = 90f
                val currentIndex = SWIPE_NAV_ROUTES.indexOf(currentRoute)
                val targetRoute = when {
                    totalDrag <= -threshold && currentIndex < SWIPE_NAV_ROUTES.lastIndex ->
                        SWIPE_NAV_ROUTES[currentIndex + 1]
                    totalDrag >= threshold && currentIndex > 0 ->
                        SWIPE_NAV_ROUTES[currentIndex - 1]
                    else -> null
                }
                totalDrag = 0f
                targetRoute?.let(onNavigate)
            },
            onDragCancel = {
                totalDrag = 0f
            },
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// BOTTOM BAR
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private data class TabItem(
    val route:       String,
    val label:       String,
    val iconDefault: ImageVector,
    val iconActive:  ImageVector,
)

private val TABS = listOf(
    TabItem(Screen.Chat.route,     "Chats",    Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
    TabItem(Screen.Spaces.route,   "Spaces",   Icons.Outlined.FolderOpen,        Icons.Filled.Folder),
    TabItem(Screen.Modes.route,    "Modes",    Icons.Outlined.Layers,            Icons.Filled.Layers),
    TabItem(Screen.Settings.route, "Settings", Icons.Outlined.Settings,          Icons.Filled.Settings),
)

@Composable
private fun OrbitBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpaceDeep),
    ) {
        // Thin top separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.TopCenter)
                .background(GlassBorder),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp, top = 4.dp),
            verticalAlignment     = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TabButton(TABS[0], currentRoute == TABS[0].route) { onNavigate(TABS[0].route) }
            TabButton(TABS[1], currentRoute == TABS[1].route) { onNavigate(TABS[1].route) }
            OrbitNavButton(onClick = { OrbitBubbleService.trigger(context) })
            TabButton(TABS[2], currentRoute == TABS[2].route) { onNavigate(TABS[2].route) }
            TabButton(TABS[3], currentRoute == TABS[3].route) { onNavigate(TABS[3].route) }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// RAISED CENTER ORBIT BUTTON
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun OrbitNavButton(onClick: () -> Unit) {
    var isActivated by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isActivated) 1.12f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "orbit_nav_scale",
    )
    val buttonColor by animateColorAsState(
        targetValue = if (isActivated) VioletCore else TextPrimary,
        animationSpec = tween(durationMillis = 220),
        label = "orbit_nav_bg",
    )
    val ringColor by animateColorAsState(
        targetValue = if (isActivated) VioletGlow.copy(alpha = 0.7f) else Color.Transparent,
        animationSpec = tween(durationMillis = 220),
        label = "orbit_nav_ring",
    )

    LaunchedEffect(isActivated) {
        if (isActivated) {
            kotlinx.coroutines.delay(1100)
            isActivated = false
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.offset(y = (-18).dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(buttonColor)
                .border(width = 2.dp, color = ringColor, shape = CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClick           = {
                        isActivated = true
                        onClick()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter            = painterResource(R.drawable.vector_logo),
                contentDescription = "Orbit",
                modifier           = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text      = "Orbit",
            style     = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color     = if (isActivated) VioletGlow else TextPrimary,
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// INDIVIDUAL TAB BUTTON
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun TabButton(
    tab: TabItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint = TextPrimary.copy(alpha = if (selected) 1f else 0.4f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector        = if (selected) tab.iconActive else tab.iconDefault,
            contentDescription = tab.label,
            tint               = tint,
            modifier           = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text       = tab.label,
            style      = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color      = tint,
        )
    }
}
