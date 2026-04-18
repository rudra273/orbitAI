package com.example.orbitai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.orbitai.R
import com.example.orbitai.core.common.OnboardingSettingsStore
import com.example.orbitai.feature.bubble.BubbleSettingsStore
import com.example.orbitai.feature.bubble.OrbitBubbleService
import com.example.orbitai.ui.theme.IsOrbitDarkTheme
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.ui.theme.TextPrimary
import com.example.orbitai.viewmodel.MemoryViewModel

private val WelcomeSurfaceLight = Color(0xFFF9F8F5)
private val WelcomeCardLight = Color(0xFFFFFFFF)
private val WelcomeCardDark = Color(0xFF1E1E1C)
private val WelcomeInkLight = Color(0xFF0D0D0D)
private val WelcomeAccent = Color(0xFF5B4FE8)
private val WelcomeSans = FontFamily.SansSerif
private val WelcomeMono = FontFamily.Monospace

private val WelcomeSurface: Color
    @Composable get() = if (IsOrbitDarkTheme) SpaceDeep else WelcomeSurfaceLight
private val WelcomeCard: Color
    @Composable get() = if (IsOrbitDarkTheme) WelcomeCardDark else WelcomeCardLight
private val WelcomeInk: Color
    @Composable get() = if (IsOrbitDarkTheme) TextPrimary else WelcomeInkLight

@Composable
fun WelcomeScreen(
    onboardingStore: OnboardingSettingsStore,
    bubbleSettingsStore: BubbleSettingsStore,
    memoryViewModel: MemoryViewModel,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    var userName by remember { mutableStateOf(onboardingStore.userName) }
    var useBubble by remember { mutableStateOf(bubbleSettingsStore.isFloatingBubbleEnabled) }
    var pendingFinish by remember { mutableStateOf(false) }

    fun completeWelcome() {
        val trimmedName = userName.trim()
        if (trimmedName.isBlank()) return

        onboardingStore.userName = trimmedName
        onboardingStore.hasCompletedWelcome = true
        memoryViewModel.setUserName(trimmedName)

        if (!useBubble) {
            bubbleSettingsStore.isFloatingBubbleEnabled = false
            OrbitBubbleService.stop(context)
        }

        onFinished()
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        bubbleSettingsStore.isFloatingBubbleEnabled = granted
        if (granted) {
            OrbitBubbleService.start(context)
        } else {
            OrbitBubbleService.stop(context)
        }
        if (pendingFinish) {
            pendingFinish = false
            completeWelcome()
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val overlayGranted = OrbitBubbleService.canDrawOverlays(context)
        if (!overlayGranted) {
            bubbleSettingsStore.isFloatingBubbleEnabled = false
            OrbitBubbleService.stop(context)
            if (pendingFinish) {
                pendingFinish = false
                completeWelcome()
            }
        } else {
            val audioGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (audioGranted) {
                bubbleSettingsStore.isFloatingBubbleEnabled = true
                OrbitBubbleService.start(context)
                if (pendingFinish) {
                    pendingFinish = false
                    completeWelcome()
                }
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    fun submit() {
        val trimmedName = userName.trim()
        if (trimmedName.isBlank()) return

        if (!useBubble) {
            completeWelcome()
            return
        }

        pendingFinish = true
        val overlayGranted = OrbitBubbleService.canDrawOverlays(context)
        val audioGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        if (!overlayGranted) {
            overlayPermissionLauncher.launch(OrbitBubbleService.overlayPermissionIntent(context))
            return
        }

        if (!audioGranted) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        bubbleSettingsStore.isFloatingBubbleEnabled = true
        OrbitBubbleService.start(context)
        pendingFinish = false
        completeWelcome()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WelcomeSurface)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(WelcomeAccent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.vector_logo),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Welcome to OrbitAI",
                    color = WelcomeInk,
                    fontFamily = WelcomeSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                )
                Text(
                    text = "Your own on-device private AI assistant.",
                    color = WelcomeInk.copy(alpha = 0.56f),
                    fontFamily = WelcomeSans,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(WelcomeCard)
                    .border(
                        width = 1.dp,
                        color = WelcomeInk.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "What should I call you?",
                        color = WelcomeInk,
                        fontFamily = WelcomeSans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                    )
                    BasicTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        singleLine = true,
                        textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                            color = WelcomeInk,
                            fontFamily = WelcomeSans,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(WelcomeInk.copy(alpha = 0.05f))
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (userName.isBlank()) {
                                    Text(
                                        text = "Enter your name",
                                        color = WelcomeInk.copy(alpha = 0.35f),
                                        fontFamily = WelcomeSans,
                                        fontSize = 15.sp,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(WelcomeAccent.copy(alpha = if (IsOrbitDarkTheme) 0.12f else 0.08f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Use Orbit Bubble",
                                    color = WelcomeInk,
                                    fontFamily = WelcomeSans,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                )
                                RecommendedTag()
                            }
                            Text(
                                text = "Use your assistant anywhere. Ask for help, capture context, or automate from any screen without leaving what you are doing.",
                                color = WelcomeInk.copy(alpha = 0.56f),
                                fontFamily = WelcomeSans,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                            )
                        }

                        Spacer(modifier = Modifier.size(12.dp))
                        WelcomeToggle(
                            checked = useBubble,
                            onCheckedChange = { useBubble = it },
                        )
                    }

                    Text(
                        text = "You can turn this off later in Settings / Orbit Bubble.",
                        color = WelcomeInk.copy(alpha = 0.50f),
                        fontFamily = WelcomeMono,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryWelcomeButton(
                label = "Continue",
                enabled = userName.trim().isNotBlank(),
                onClick = ::submit,
            )
            Text(
                text = "Orbit runs fully on-device. You can also add Gemini or GPT API later if you want.",
                color = WelcomeInk.copy(alpha = 0.42f),
                fontFamily = WelcomeMono,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun RecommendedTag() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(WelcomeAccent.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Recommended",
            color = WelcomeAccent,
            fontFamily = WelcomeMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun WelcomeToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val knobColor = if (checked && IsOrbitDarkTheme) SpaceDeep else Color.White

    Box(
        modifier = Modifier
            .size(width = 42.dp, height = 24.dp)
            .clip(CircleShape)
            .background(if (checked) WelcomeAccent else WelcomeInk.copy(alpha = 0.12f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(horizontal = 3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(knobColor),
        )
    }
}

@Composable
private fun PrimaryWelcomeButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) WelcomeInk else WelcomeInk.copy(alpha = 0.16f))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(PaddingValues(horizontal = 18.dp, vertical = 16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (enabled) {
                if (IsOrbitDarkTheme) SpaceDeep else Color.White
            } else {
                WelcomeInk.copy(alpha = 0.36f)
            },
            fontFamily = WelcomeSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}
