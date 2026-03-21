package com.example.orbitai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.data.LlmModel
import com.example.orbitai.data.TokenStore
import com.example.orbitai.viewmodel.DownloadViewModel
import com.example.orbitai.ui.theme.*

@Composable
fun DeveloperSettingsScreen(
    tokenStore: TokenStore,
    downloadViewModel: DownloadViewModel,
    onBack: () -> Unit,
) {
    SettingsSubScreen(
        title = "Developer",
        icon = Icons.Default.Code,
        accent = Color(0xFFF472B6),
        onBack = onBack,
    ) {
        SettingsDescription("Configure provider keys. Gemini appears in chat only when API key and model are set.")

        SettingsSectionLabel("Gemini API")
        GeminiApiConfigCard(tokenStore)

        SettingsSectionLabel("HuggingFace")

        HuggingFaceTokenCard(tokenStore)

        SettingsSectionLabel("Custom Model URL")

        CustomUrlCard { url, fileName ->
            val normalizedFileName = normalizeModelFileName(fileName)
            val custom = LlmModel(
                id = "custom-${System.currentTimeMillis()}",
                displayName = normalizedFileName.removeSuffix(".litertlm").removeSuffix(".task"),
                fileName = normalizedFileName,
                description = "Custom model",
                paramCount = "?",
                format = inferModelFormat(normalizedFileName),
            )
            downloadViewModel.startDownload(custom, url)
        }
    }
}

@Composable
private fun GeminiApiConfigCard(tokenStore: TokenStore) {
    var modelName by remember { mutableStateOf(tokenStore.geminiModelName) }
    var apiKey by remember { mutableStateOf(tokenStore.geminiApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(tokenStore.hasGeminiConfig()) }

    GlassCard(accent = Color(0xFF60A5FA)) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (saved) Color(0xFF34D399) else TextMuted.copy(0.4f)),
            )
            Text(
                if (saved) "Gemini configured" else "Gemini not configured",
                style = MaterialTheme.typography.labelLarge,
                color = if (saved) Color(0xFF34D399) else TextMuted,
            )
        }

        Spacer(Modifier.height(12.dp))

        TextField(
            value = modelName,
            onValueChange = {
                modelName = it.lowercase()
                saved = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GlassWhite4),
            placeholder = {
                Text(
                    "model name",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = TextMuted,
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = Color(0xFF60A5FA),
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Next,
            ),
        )

        Spacer(Modifier.height(8.dp))

        TextField(
            value = apiKey,
            onValueChange = {
                apiKey = it
                saved = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GlassWhite4),
            placeholder = {
                Text(
                    "api key",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = TextMuted,
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = Color(0xFF60A5FA),
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            singleLine = true,
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle visibility",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OrbitPrimaryButton(
                label = if (saved) "Saved" else "Save Gemini",
                enabled = modelName.isNotBlank() && apiKey.isNotBlank() && !saved,
                accent = Color(0xFF60A5FA),
                onClick = {
                    tokenStore.geminiModelName = modelName
                    tokenStore.geminiApiKey = apiKey
                    saved = true
                },
            )
            if (saved) {
                OrbitDestructiveButton(
                    label = "Clear",
                    onClick = {
                        modelName = ""
                        apiKey = ""
                        tokenStore.geminiModelName = ""
                        tokenStore.geminiApiKey = ""
                        saved = false
                    },
                )
            }
        }
    }
}

@Composable
private fun HuggingFaceTokenCard(tokenStore: TokenStore) {
    var token by remember { mutableStateOf(tokenStore.huggingFaceToken) }
    var showToken by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(tokenStore.hasToken()) }

    GlassCard(accent = Color(0xFFF472B6)) {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (saved) Color(0xFF34D399) else TextMuted.copy(0.4f)),
            )
            Text(
                if (saved) "Token saved" else "No token set",
                style = MaterialTheme.typography.labelLarge,
                color = if (saved) Color(0xFF34D399) else TextMuted,
            )
        }

        Spacer(Modifier.height(12.dp))

        TextField(
            value = token,
            onValueChange = { token = it; saved = false },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GlassWhite4),
            placeholder = {
                Text(
                    "hf_xxxxxxxxxxxxxxxx",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = TextMuted,
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = Color(0xFFF472B6),
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            singleLine = true,
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(
                        if (showToken) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle visibility",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OrbitPrimaryButton(
                label = if (saved) "Saved" else "Save Token",
                enabled = token.isNotBlank() && !saved,
                accent = Color(0xFFF472B6),
                onClick = {
                    tokenStore.huggingFaceToken = token
                    saved = true
                },
            )
            if (saved) {
                OrbitDestructiveButton(
                    label = "Clear",
                    onClick = {
                        token = ""
                        tokenStore.huggingFaceToken = ""
                        saved = false
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "1. Go to huggingface.co/settings/tokens\n" +
                "2. Create a token with read access\n" +
                "3. Accept each model's license on HuggingFace",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted.copy(0.65f),
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun CustomUrlCard(onDownload: (url: String, fileName: String) -> Unit) {
    var url by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }

    GlassCard(accent = Color(0xFFF472B6)) {
        Text(
            "Any compatible Task or LiteRT-LM file",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Paste any MediaPipe LLM model URL (.task or .litertlm) below.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
        Spacer(Modifier.height(12.dp))

        TextField(
            value = url,
            onValueChange = { url = it },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(GlassWhite4),
            placeholder = {
                Text(
                    "https://.../model.task or model.litertlm",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextMuted,
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = VioletBright,
            ),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            maxLines = 2,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )

        Spacer(Modifier.height(8.dp))

        TextField(
            value = fileName,
            onValueChange = { fileName = it },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(GlassWhite4),
            placeholder = {
                Text(
                    "Save as (e.g. mymodel.task)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = VioletBright,
            ),
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done,
            ),
        )

        Spacer(Modifier.height(12.dp))

        OrbitPrimaryButton(
            label = "Download Custom Model",
            enabled = url.isNotBlank() && fileName.isNotBlank(),
            onClick = {
                val name = normalizeModelFileName(fileName)
                if (url.isNotBlank() && name.isNotBlank()) onDownload(url.trim(), name)
            },
        )
    }
}
