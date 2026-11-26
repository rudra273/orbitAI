package com.example.orbitai.ui.screens

import com.example.orbitai.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orbitai.feature.chat.markdown.Align
import com.example.orbitai.feature.chat.markdown.CodeHighlighter
import com.example.orbitai.feature.chat.markdown.MarkdownParser
import com.example.orbitai.feature.chat.markdown.MdBlock
import com.example.orbitai.feature.chat.markdown.TokenKind
import com.example.orbitai.ui.theme.*

/** Renders a chat message's markdown. Links ask for confirmation before opening. */
@Composable
fun MarkdownMessageText(
    content: String,
    textColor: Color = TextPrimary,
) {
    val blocks = remember(content) { MarkdownParser.parseBlocks(content) }
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current

    pendingUrl?.let { url ->
        AlertDialog(
            onDismissRequest = { pendingUrl = null },
            title = { Text(stringResource(R.string.open_link)) },
            text = { Text(url) },
            confirmButton = {
                TextButton(onClick = {
                    pendingUrl = null
                    runCatching { uriHandler.openUri(url) }
                }) { Text(stringResource(R.string.open)) }
            },
            dismissButton = { TextButton(onClick = { pendingUrl = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    val body = MaterialTheme.typography.bodyLarge
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> OrbitTheme.typography.title2
                        2 -> OrbitTheme.typography.title3.copy(fontFamily = OrbitEditorialFont)
                        else -> OrbitTheme.typography.headline
                    }
                    Text(
                        text = rememberInline(block.text) { pendingUrl = it },
                        style = style,
                        color = textColor,
                        lineHeight = (style.fontSize.value + 7).sp,
                        modifier = Modifier.padding(top = if (block.level <= 2) 6.dp else 2.dp),
                    )
                }

                is MdBlock.Paragraph -> Text(
                    text = rememberInline(block.text) { pendingUrl = it },
                    style = body,
                    color = textColor,
                    lineHeight = 25.sp,
                )

                is MdBlock.ListItem -> Row(
                    modifier = Modifier.padding(start = (block.depth * 18).dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = when {
                            block.checked == true -> "☑"
                            block.checked == false -> "☐"
                            block.number != null -> "${block.number}."
                            block.depth > 0 -> "◦"
                            else -> "•"
                        },
                        style = body,
                        color = textColor,
                        lineHeight = 25.sp,
                    )
                    Text(
                        text = rememberInline(block.text) { pendingUrl = it },
                        style = body,
                        color = textColor,
                        lineHeight = 25.sp,
                    )
                }

                is MdBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(VioletCore.copy(alpha = 0.6f)),
                    )
                    Text(
                        text = rememberInline(block.text) { pendingUrl = it },
                        style = body.copy(fontStyle = FontStyle.Italic),
                        color = textColor.copy(alpha = 0.8f),
                        lineHeight = 25.sp,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }

                is MdBlock.Code -> CodeBlockView(language = block.language, code = block.code)

                is MdBlock.Table -> MarkdownTable(block, textColor) { pendingUrl = it }

                MdBlock.Rule -> Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .height(1.dp)
                        .background(GlassBorder),
                )
            }
        }
    }
}

@Composable
private fun rememberInline(text: String, onLink: (String) -> Unit): AnnotatedString {
    val linkColor = VioletCore
    return remember(text) {
        buildAnnotatedString {
            MarkdownParser.parseInline(text).forEach { span ->
                val style = SpanStyle(
                    fontWeight = if (span.bold) FontWeight.Bold else null,
                    fontStyle = if (span.italic) FontStyle.Italic else null,
                    textDecoration = if (span.strike) TextDecoration.LineThrough else null,
                    fontFamily = if (span.code) FontFamily.Monospace else null,
                    background = if (span.code) SpaceDust else Color.Unspecified,
                    color = if (span.code) VioletCore else Color.Unspecified,
                )
                val url = span.url
                if (url != null) {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = url,
                            styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                            linkInteractionListener = { onLink(url) },
                        )
                    ) { withStyle(style) { append(span.text) } }
                } else {
                    withStyle(style) { append(span.text) }
                }
            }
        }
    }
}

@Composable
private fun MarkdownTable(table: MdBlock.Table, textColor: Color, onLink: (String) -> Unit) {
    val headerBg = if (IsOrbitDarkTheme) Color(0xFF1F1F1D) else Color(0xFFEDECE6)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState()),
    ) {
        Column {
            (listOf(table.header) + table.rows).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .background(if (rowIndex == 0) headerBg else Color.Transparent)
                        .height(IntrinsicSize.Min),
                ) {
                    row.forEachIndexed { col, cell ->
                        Text(
                            text = rememberInline(cell, onLink),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (rowIndex == 0) FontWeight.SemiBold else null,
                            ),
                            color = textColor,
                            textAlign = when (table.alignments.getOrNull(col)) {
                                Align.CENTER -> TextAlign.Center
                                Align.END -> TextAlign.End
                                else -> TextAlign.Start
                            },
                            modifier = Modifier
                                .widthIn(min = 72.dp, max = 220.dp)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
                if (rowIndex < table.rows.size) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(GlassBorder))
                }
            }
        }
    }
}

@Composable
fun CodeBlockView(language: String, code: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    val codeBlockBg   = if (IsOrbitDarkTheme) Color(0xFF1A1A18) else Color(0xFFF0EFE9)
    val codeHeaderBg  = if (IsOrbitDarkTheme) Color(0xFF141413) else Color(0xFFE8E7E1)
    val codeTextColor = if (IsOrbitDarkTheme) Color(0xFFE8E6E1) else Color(0xFF1A1A1A)
    val keywordColor  = if (IsOrbitDarkTheme) Color(0xFFC792EA) else Color(0xFF7C3AED)
    val stringColor   = if (IsOrbitDarkTheme) Color(0xFFC3E88D) else Color(0xFF15803D)
    val commentColor  = if (IsOrbitDarkTheme) Color(0xFF8A8A85) else Color(0xFF8A8A85)
    val numberColor   = if (IsOrbitDarkTheme) Color(0xFFF78C6C) else Color(0xFFC2410C)

    val highlighted = remember(code, language, IsOrbitDarkTheme) {
        buildAnnotatedString {
            append(code)
            CodeHighlighter.tokens(code, language).forEach { token ->
                val color = when (token.kind) {
                    TokenKind.KEYWORD -> keywordColor
                    TokenKind.STRING -> stringColor
                    TokenKind.COMMENT -> commentColor
                    TokenKind.NUMBER -> numberColor
                }
                addStyle(
                    SpanStyle(color = color, fontStyle = if (token.kind == TokenKind.COMMENT) FontStyle.Italic else null),
                    token.start,
                    token.end,
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(codeBlockBg)
            .border(
                width = 1.dp,
                color = GlassBorder,
                shape = RoundedCornerShape(10.dp),
            ),
    ) {
        // Header: language + copy
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(codeHeaderBg)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text       = language.ifBlank { "code" },
                style      = MaterialTheme.typography.labelSmall,
                color      = TextMuted,
                fontFamily = FontFamily.Monospace,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (copied) Success.copy(0.15f) else GlassWhite8)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                    ) {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = if (copied) "Copied!" else "Copy",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (copied) Success else TextMuted,
                )
            }
        }

        // Code content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            SelectionContainer {
                Text(
                    text     = highlighted,
                    style    = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp,
                    ),
                    color    = codeTextColor,
                    softWrap = false,
                )
            }
        }
    }

    // Auto-reset "Copied!" label after 2 s
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }
}
