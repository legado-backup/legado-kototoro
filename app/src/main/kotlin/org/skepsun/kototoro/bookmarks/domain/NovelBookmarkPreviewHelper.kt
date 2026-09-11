package org.skepsun.kototoro.bookmarks.domain

import android.util.Base64
import androidx.collection.LruCache
import org.jsoup.Jsoup

private val novelBookmarkPreviewCache = LruCache<String, String>(256)

/**
 * Extracts a clean, human-readable plain text preview from a bookmark's [imageUrl] field.
 *
 * For novel bookmarks, [imageUrl] stores either the text snippet directly, or an HTML fragment,
 * or a data URL (e.g. data:text/html;base64,...).
 * If [imageUrl] is a media/file URL (e.g. http/https/file) or empty, an empty string is returned.
 */
fun extractNovelBookmarkPreview(imageUrl: String?): String {
    if (imageUrl.isNullOrBlank()) return ""
    novelBookmarkPreviewCache.get(imageUrl)?.let { return it }

    val trimmed = imageUrl.trim()
    val result = try {
        when {
            trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("file://", ignoreCase = true) ||
                trimmed.startsWith("content://", ignoreCase = true) -> {
                // This is a file/network media URL (e.g. manga image snapshot or book cover), not novel text
                ""
            }
            trimmed.startsWith("data:text/html", ignoreCase = true) -> {
                val base64Data = trimmed.substringAfter("base64,", "")
                if (base64Data.isNotEmpty()) {
                    val htmlBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    val html = String(htmlBytes, Charsets.UTF_8)
                    extractTextFromHtml(html)
                } else {
                    ""
                }
            }
            trimmed.contains('<') && trimmed.contains('>') -> {
                extractTextFromHtml(trimmed)
            }
            else -> {
                trimmed.replace(Regex("[\\r\\n\\t]+"), " ").take(200).trim()
            }
        }
    } catch (_: Throwable) {
        ""
    }

    novelBookmarkPreviewCache.put(imageUrl, result)
    return result
}

private fun extractTextFromHtml(html: String): String {
    return try {
        val doc = Jsoup.parse(html)
        doc.select("script, style, meta, link").remove()
        val text = doc.body().text().trim()
        text.replace(Regex("\\s+"), " ").take(200).trim()
    } catch (_: Throwable) {
        ""
    }
}
