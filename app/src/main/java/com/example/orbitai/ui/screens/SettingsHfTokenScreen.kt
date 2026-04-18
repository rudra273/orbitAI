package com.example.orbitai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.core.common.TokenStore
import com.example.orbitai.ui.theme.IsOrbitDarkTheme
import com.example.orbitai.ui.theme.SpaceDeep
import com.example.orbitai.ui.theme.TextPrimary

private val SettingsSurfaceLight = Color(0xFFF9F8F5)
private val SettingsCardLight = Color(0xFFFFFFFF)
private val SettingsCardDark = Color(0xFF1E1E1C)
private val SettingsInkLight = Color(0xFF0D0D0D)
private val SettingsGreen = Color(0xFF17A865)
private val SettingsRed = Color(0xFFD94F4F)
private val SettingsBlue = Color(0xFF3B82F6)
private val SettingsSans = FontFamily.SansSerif
private val SettingsMono = FontFamily.Monospace

private val SettingsSurface: Color
    @Composable get() = if (IsOrbitDarkTheme) SpaceDeep else SettingsSurfaceLight
private val SettingsCard: Color
    @Composable get() = if (IsOrbitDarkTheme) SettingsCardDark else SettingsCardLight
private val SettingsInk: Color
    @Composable get() = if (IsOrbitDarkTheme) TextPrimary else SettingsInkLight

@Composable
fun SettingsHfTokenScreen(
    tokenStore: TokenStore,
    onBack: () -> Unit,
) {
    var token by remember { mutableStateOf(tokenStore.huggingFaceToken) }
    var tokenSaved by remember { mutableStateOf(tokenStore.hasToken()) }
    var showToken by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsSurface),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = SettingsInk.copy(alpha = 0.40f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "Models",
                    color = SettingsInk.copy(alpha = 0.40f),
                    fontFamily = SettingsMono,
                    fontSize = 13.sp,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 8.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "HuggingFace Token",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = SettingsInk,
                    fontFamily = SettingsSans,
                )
                Text(
                    text = "Authenticate to download gated models",
                    color = SettingsInk.copy(alpha = 0.35f),
                    fontFamily = SettingsMono,
                    fontSize = 11.sp,
                )
            }
        }

        item {
            FlatCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Access Token",
                            color = SettingsInk,
                            fontFamily = SettingsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                        )
                        Text(
                            text = "Paste your HuggingFace access token securely.",
                            color = SettingsInk.copy(alpha = 0.35f),
                            fontFamily = SettingsMono,
                            fontSize = 11.sp,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(SettingsInk.copy(alpha = 0.06f))
                                .padding(start = 10.dp, end = 4.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BasicTextField(
                                    value = token,
                                    onValueChange = { entered ->
                                        token = entered
                                        tokenSaved = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = true,
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = SettingsMono,
                                        color = SettingsInk,
                                        fontSize = 12.sp,
                                    ),
                                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                                    cursorBrush = SolidColor(SettingsInk),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done,
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (token.isEmpty()) {
                                                Text(
                                                    "hf_xxx...",
                                                    color = SettingsInk.copy(alpha = 0.45f),
                                                    fontFamily = SettingsMono,
                                                    fontSize = 12.sp,
                                                )
                                            }
                                            innerTextField()
                                        }
                                    },
                                )

                                IconButton(onClick = { showToken = !showToken }) {
                                    Icon(
                                        imageVector = if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = SettingsInk.copy(alpha = 0.55f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }

                        SmallFilledButton(
                            label = if (tokenSaved) "Saved" else "Save",
                            color = SettingsInk,
                            enabled = token.isNotBlank() && !tokenSaved,
                            onClick = {
                                tokenStore.huggingFaceToken = token
                                tokenSaved = true
                            },
                        )

                        SmallOutlineButton(
                            label = "Clear",
                            color = SettingsRed,
                            onClick = {
                                token = ""
                                tokenStore.huggingFaceToken = ""
                                tokenSaved = false
                                showToken = true
                            },
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            SectionHeader("HOW IT WORKS")
        }

        item {
            FlatCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GuideStep(
                        step = "1",
                        title = "Generate Token",
                        description = "You need a free HuggingFace account to generate an Access Token.",
                        buttonLabel = "Create Token",
                        onClick = { uriHandler.openUri("https://huggingface.co/settings/tokens") }
                    )
                    GuideStep(
                        step = "2",
                        title = "Check Model Licenses",
                        description = "Models like Gemma 4 require you to read and accept their license before downloading.",
                        buttonLabel = "Accept Gemma 4 License",
                        onClick = { uriHandler.openUri("https://huggingface.co/google/gemma-3-2b-it") } // They referred to Gemma 4 e2b, but its currently titled Gemma-3n-2b or Gemma-4 on HF
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideStep(
    step: String,
    title: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SettingsInk.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = SettingsInk,
                fontFamily = SettingsMono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                color = SettingsInk,
                fontFamily = SettingsSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            )
            Text(
                text = description,
                color = SettingsInk.copy(alpha = 0.6f),
                fontFamily = SettingsSans,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SettingsBlue.copy(alpha = 0.1f))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = buttonLabel,
                    color = SettingsBlue,
                    fontFamily = SettingsSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = SettingsBlue,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun FlatCard(
    padding: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SettingsCard)
            .border(
                width = 1.dp,
                color = SettingsInk.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(padding),
        content = content,
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = SettingsInk.copy(alpha = 0.30f),
        fontFamily = SettingsMono,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
    )
}

@Composable
private fun SmallFilledButton(
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (enabled) color else color.copy(alpha = 0.32f))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (IsOrbitDarkTheme) SpaceDeep else Color.White,
            fontFamily = SettingsMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SmallOutlineButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(color.copy(alpha = 0.10f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = color,
            fontFamily = SettingsMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
