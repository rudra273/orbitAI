package com.example.orbitai.feature.chat.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import androidx.core.net.toUri
import com.example.orbitai.feature.chat.Chat
import com.example.orbitai.feature.chat.markdownToPlainText
import java.io.OutputStream

/**
 * Writes a chat to an A4 PDF with [PdfDocument]. Text is laid out with [StaticLayout] and split
 * between pages on line boundaries, so no line is cut off.
 */
class ChatPdfWriter(private val context: Context) {

    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 48f
    private val contentWidth = (pageWidth - 2 * margin).toInt()

    private val titlePaint = textPaint(20f, Typeface.DEFAULT_BOLD)
    private val subtitlePaint = textPaint(10f, Typeface.DEFAULT, 0xFF666666.toInt())
    private val speakerPaint = textPaint(11f, Typeface.DEFAULT_BOLD, 0xFF3A3A8C.toInt())
    private val bodyPaint = textPaint(11f, Typeface.DEFAULT)

    private val document = PdfDocument()
    private var page: PdfDocument.Page? = null
    private var pageNumber = 0
    private var y = margin

    fun write(chat: Chat, modelName: String?, includeImages: Boolean, out: OutputStream) {
        try {
            newPage()
            drawText(ChatExport.displayTitle(chat), titlePaint)
            drawText(ChatExport.subtitle(chat, modelName), subtitlePaint)
            ChatExport.exportable(chat).forEach { message ->
                y += 14f
                drawText(ChatExport.speaker(message), speakerPaint)
                y += 2f
                if (includeImages) message.imageUris.forEach { drawImage(it.toUri()) }
                else ChatExport.imageNote(message)?.let { drawText(it, subtitlePaint) }
                if (message.content.isNotBlank()) drawText(markdownToPlainText(message.content), bodyPaint)
            }
            page?.let(document::finishPage)
            page = null
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    private fun textPaint(size: Float, typeface: Typeface, color: Int = 0xFF111111.toInt()) =
        TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.typeface = typeface
            this.color = color
        }

    private fun newPage() {
        page?.let(document::finishPage)
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        y = margin
    }

    private val canvas: Canvas get() = page!!.canvas
    private val bottom get() = pageHeight - margin

    /** Draws [text], moving to a new page between lines whenever the next line doesn't fit. */
    private fun drawText(text: String, paint: TextPaint) {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1f)
            .build()
        var line = 0
        while (line < layout.lineCount) {
            if (y + layout.getLineBottom(line) - layout.getLineTop(line) > bottom) newPage()
            val first = line
            val top = layout.getLineTop(first)
            while (line < layout.lineCount && y + layout.getLineBottom(line) - top <= bottom) line++
            if (line == first) line++ // A single line taller than a page: draw it anyway.
            val height = layout.getLineBottom(line - 1) - top
            canvas.withTranslation(margin, y - top) {
                clipRect(0, top, contentWidth, top + height)
                layout.draw(this)
            }
            y += height
        }
    }

    private fun drawImage(uri: Uri) {
        val bitmap = runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val scale = minOf(1f, 1200f / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
            }
        }.getOrNull()
        if (bitmap == null) {
            drawText("[Image no longer available]", subtitlePaint)
            return
        }
        try {
            val maxHeight = 320f
            val scale = minOf(contentWidth.toFloat() / bitmap.width, maxHeight / bitmap.height)
            val width = bitmap.width * scale
            val height = bitmap.height * scale
            if (y + height > bottom) newPage()
            canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), RectF(margin, y, margin + width, y + height), null)
            y += height + 6f
        } finally {
            bitmap.recycle()
        }
    }
}
