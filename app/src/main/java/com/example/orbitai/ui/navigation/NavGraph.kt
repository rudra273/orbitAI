package com.example.orbitai.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.example.orbitai.ui.screens.AgentsScreen
import com.example.orbitai.ui.screens.ChatScreen
import com.example.orbitai.ui.screens.HomeScreen
import com.example.orbitai.ui.screens.SpaceDetailScreen
import com.example.orbitai.ui.screens.SpacesScreen
import com.example.orbitai.ui.screens.SettingsScreen
import com.example.orbitai.ui.theme.*
import com.example.orbitai.viewmodel.AgentsViewModel
import com.example.orbitai.viewmodel.ChatViewModel
import com.example.orbitai.viewmodel.DownloadViewModel
import com.example.orbitai.viewmodel.MemoryViewModel
import com.example.orbitai.viewmodel.SpacesViewModel

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ROUTE DEFINITIONS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

sealed class Screen(val route: String) {
    // ── 4 primary tabs ──────────────────────────────────────────────────────
    data object Chat     : Screen("chat_list")   // renamed from Home for clarity
    data object Spaces   : Screen("spaces")
    data object Agents   : Screen("agents")
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
    data object SettingsInference : Screen("settings/inference")
    data object SettingsMemory    : Screen("settings/memory")      // moved from tab
    data object SettingsRag       : Screen("settings/rag")
    data object SettingsDeveloper : Screen("settings/developer")
}

private val TAB_ROUTES = setOf(
    Screen.Chat.route,
    Screen.Spaces.route,
    Screen.Agents.route,
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
    spacesViewModel:   SpacesViewModel,
    agentsViewModel:   AgentsViewModel,
    memoryViewModel:   MemoryViewModel,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route
    val showBottomBar  = currentRoute in TAB_ROUTES

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
        NavHost(
            navController    = navController,
            startDestination = Screen.Chat.route,
            modifier         = Modifier.padding(innerPadding),
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

            composable(Screen.Agents.route) {
                AgentsScreen(viewModel = agentsViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    downloadViewModel = downloadViewModel,
                    memoryViewModel   = memoryViewModel,
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
                    onBack    = { navController.popBackStack() },
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

            // ── Settings sub-screens ───────────────────────────────────────
            // Each sub-screen is a separate composable — wire them here as you build them out.
            // Example pattern shown; replace with your actual composable calls:

            composable(Screen.SettingsModel.route) {
                // ModelSettingsScreen(viewModel = downloadViewModel, onBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsInference.route) {
                // InferenceSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsMemory.route) {
                // MemorySettingsScreen(viewModel = memoryViewModel, onBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsRag.route) {
                // RagSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SettingsDeveloper.route) {
                // DeveloperSettingsScreen(onBack = { navController.popBackStack() })
            }
        }
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
    TabItem(Screen.Chat.route,     "Chat",     Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
    TabItem(Screen.Spaces.route,   "Spaces",   Icons.Outlined.FolderOpen,        Icons.Filled.Folder),
    TabItem(Screen.Agents.route,   "Agents",   Icons.Outlined.Person,            Icons.Filled.Person),
    TabItem(Screen.Settings.route, "Settings", Icons.Outlined.Settings,          Icons.Filled.Settings),
)

@Composable
private fun OrbitBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    // Outer container — provides the nav scrim fade + horizontal padding
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, SpaceDeep.copy(alpha = 0.97f)),
                    startY = 0f,
                    endY   = Float.POSITIVE_INFINITY,
                )
            )
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp, top = 8.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Glassy pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                // Glass fill
                .background(
                    color = SpaceDust.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(24.dp),
                )
                // Violet glow halo underneath
                .drawBehind {
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            asFrameworkPaint().apply {
                                isAntiAlias = true
                                color       = android.graphics.Color.TRANSPARENT
                                setShadowLayer(
                                    32f, 0f, 4f,
                                    VioletGlow
                                        .copy(alpha = 0.35f)
                                        .toArgb(),
                                )
                            }
                        }
                        canvas.drawRoundRect(
                            left   = 0f, top = 0f,
                            right  = size.width, bottom = size.height,
                            radiusX = 24.dp.toPx(), radiusY = 24.dp.toPx(),
                            paint  = paint,
                        )
                    }
                }
                // Glass border
                .then(
                    Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                GlassBorder,
                                GlassBorder.copy(alpha = 0.06f),
                            )
                        ),
                        shape = RoundedCornerShape(24.dp),
                    )
                ),
        ) {
            // Stroke ring — drawn as a 1dp border inside the pill
            Surface(
                modifier        = Modifier.fillMaxSize(),
                shape           = RoundedCornerShape(24.dp),
                color           = Color.Transparent,
                border          = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(GlassBorder, GlassBorder.copy(0.04f), GlassBorder)
                    ),
                ),
            ) {
                Row(
                    modifier              = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TABS.forEach { tab ->
                        val selected = currentRoute == tab.route
                        TabButton(
                            tab      = tab,
                            selected = selected,
                            onClick  = { onNavigate(tab.route) },
                        )
                    }
                }
            }
        }
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
    val iconTint by animateColorAsState(
        targetValue   = if (selected) VioletBright else TextMuted,
        animationSpec = tween(200),
        label         = "tab_tint",
    )
    val labelColor by animateColorAsState(
        targetValue   = if (selected) VioletBright else TextMuted,
        animationSpec = tween(200),
        label         = "tab_label",
    )
    val bgAlpha by animateFloatAsState(
        targetValue   = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label         = "tab_bg",
    )
    val indicatorWidth by animateDpAsState(
        targetValue   = if (selected) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label         = "tab_indicator",
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(VioletGlowSoft.copy(alpha = VioletGlowSoft.alpha * bgAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector        = if (selected) tab.iconActive else tab.iconDefault,
            contentDescription = tab.label,
            tint               = iconTint,
            modifier           = Modifier.size(22.dp),
        )
        Text(
            text       = tab.label,
            fontSize   = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color      = labelColor,
            maxLines   = 1,
        )
        // Active indicator dot
        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, VioletCore, Color.Transparent)
                    )
                )
        )
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// HELPERS
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

@Composable
private fun animateColorAsState(
    targetValue: Color,
    animationSpec: androidx.compose.animation.core.AnimationSpec<Color> = tween(200),
    label: String,
): State<Color> = androidx.compose.animation.animateColorAsState(
    targetValue   = targetValue,
    animationSpec = animationSpec,
    label         = label,
)