package org.skepsun.kototoro.notes.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import org.skepsun.kototoro.BuildConfig
import org.skepsun.kototoro.core.util.ext.toFileNameSafe
import org.skepsun.kototoro.notes.domain.BookNoteItem
import org.skepsun.kototoro.parsers.model.Content
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BookNotesExportHelper {

    fun generateMarkdown(manga: Content, notes: List<BookNoteItem>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()

        sb.appendLine("# 《${manga.title}》 读书笔记")
        sb.appendLine()
        if (manga.authors.isNotEmpty()) {
            sb.appendLine("> **作者**：${manga.authors.joinToString(" / ")}")
        }
        sb.appendLine("> **导出来源**：Kototoro 笔记中心")
        sb.appendLine("> **导出时间**：${dateFormat.format(Date())}")
        sb.appendLine("> **笔记总数**：${notes.size} 条")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        val grouped = notes.groupBy { it.chapterTitle }
        grouped.forEach { (chapter, chapterNotes) ->
            sb.appendLine("## $chapter")
            sb.appendLine()
            chapterNotes.forEach { note ->
                val timeStr = if (note.createdAt > 0) dateFormat.format(Date(note.createdAt)) else ""
                when (note) {
                    is BookNoteItem.NovelHighlight -> {
                        sb.appendLine("> ${note.text}")
                        sb.appendLine()
                        if (!note.note.isNullOrBlank()) {
                            sb.appendLine("💭 **想法**：${note.note}")
                            sb.appendLine()
                        }
                        if (timeStr.isNotBlank()) {
                            sb.appendLine("*$timeStr*")
                            sb.appendLine()
                        }
                    }
                    is BookNoteItem.BookmarkEntry -> {
                        val progressStr = if (note.percent > 0) " (进度 ${(note.percent * 100).toInt()}%)" else ""
                        sb.appendLine("🔖 **书签**：第 ${note.page + 1} 页$progressStr")
                        sb.appendLine()
                        if (timeStr.isNotBlank()) {
                            sb.appendLine("*$timeStr*")
                            sb.appendLine()
                        }
                    }
                }
            }
        }

        return sb.toString()
    }

    fun copyToClipboard(context: Context, manga: Content, notes: List<BookNoteItem>) {
        val md = generateMarkdown(manga, notes)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("notes_export", md))
        Toast.makeText(context, "已复制全书笔记 Markdown 格式", Toast.LENGTH_SHORT).show()
    }

    fun shareMarkdownFile(context: Context, manga: Content, notes: List<BookNoteItem>) {
        try {
            val md = generateMarkdown(manga, notes)
            val exportDir = File(context.cacheDir, "notes_export").apply { mkdirs() }
            val safeTitle = manga.title.toFileNameSafe().ifBlank { "book" }.take(50)
            val file = File(exportDir, "《${safeTitle}》读书笔记.md")
            file.writeText(md, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.files",
                file,
            )

            ShareCompat.IntentBuilder(context)
                .setType("text/markdown")
                .setStream(uri)
                .setChooserTitle("分享《${manga.title}》笔记")
                .startChooser()
        } catch (e: Exception) {
            Toast.makeText(context, "分享失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun writeMarkdownToUri(context: Context, uri: Uri, manga: Content, notes: List<BookNoteItem>): Boolean {
        return try {
            val md = generateMarkdown(manga, notes)
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(md.toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(context, "笔记已成功保存到文件", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
