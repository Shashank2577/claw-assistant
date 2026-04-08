package com.phoneclaw.ai.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle
import com.phoneclaw.ai.ui.theme.OpenClawAITheme
import com.phoneclaw.ai.ui.theme.customColors

/**
 * Renders Markdown content using the compose-richtext library.
 *
 * Supports headers, bold, italic, code blocks (monospace + surfaceVariant
 * background), lists, links, and tables. Links are coloured with [linkColor]
 * which defaults to the theme primary.
 *
 * @param text        Markdown string to render.
 * @param modifier    Layout modifier.
 * @param smallFontSize  Use bodyMedium sizing instead of bodyLarge.
 * @param textColor   Override text color (defaults to onSurface).
 * @param linkColor   Override link color (defaults to theme customColors.linkColor).
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    smallFontSize: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.customColors.linkColor,
) {
    val fontSize =
        if (smallFontSize) MaterialTheme.typography.bodyMedium.fontSize
        else MaterialTheme.typography.bodyLarge.fontSize

    CompositionLocalProvider {
        ProvideTextStyle(
            value = TextStyle(
                fontSize = fontSize,
                lineHeight = fontSize * if (smallFontSize) 1.4f else 1.5f,
                color = textColor,
                letterSpacing = 0.2.sp,
            )
        ) {
            RichText(
                modifier = modifier,
                style = RichTextStyle(
                    codeBlockStyle = CodeBlockStyle(
                        textStyle = TextStyle(
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.4f,
                        )
                    ),
                    stringStyle = RichTextStringStyle(
                        linkStyle = TextLinkStyles(style = SpanStyle(color = linkColor))
                    ),
                ),
            ) {
                Markdown(content = text)
            }
        }
    }
}

@Preview(showBackground = true, name = "MarkdownText - Light")
@Composable
private fun MarkdownTextPreview() {
    OpenClawAITheme {
        MarkdownText(
            text = """
                # Hello OpenClaw

                This is **bold** and *italic* text.

                - Item one
                - Item two

                `inline code` and a [link](https://example.com).

                ```kotlin
                val greeting = "Hello, World!"
                println(greeting)
                ```
            """.trimIndent()
        )
    }
}

@Preview(showBackground = true, name = "MarkdownText - Small Font")
@Composable
private fun MarkdownTextSmallPreview() {
    OpenClawAITheme {
        MarkdownText(
            text = "**Summary:** Token count is `1,234` tokens.",
            smallFontSize = true,
        )
    }
}
