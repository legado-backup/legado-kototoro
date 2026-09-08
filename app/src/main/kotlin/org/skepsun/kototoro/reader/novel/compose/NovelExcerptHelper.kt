package org.skepsun.kototoro.reader.novel.compose

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.skepsun.kototoro.BuildConfig
import org.skepsun.kototoro.R
import org.skepsun.kototoro.core.util.ShareHelper
import java.io.File

object NovelExcerptHelper {

    suspend fun saveExcerptToGallery(
        context: Context,
        data: NovelExcerptData,
        configuration: NovelExcerptConfiguration,
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = NovelExcerptCardRenderer.render(data, configuration)
            val filename = "kototoro_excerpt_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Kototoro")
                }
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore insert failed")
            context.contentResolver.openOutputStream(uri)?.use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            } ?: error("Could not open gallery output")

            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.novel_excerpt_saved, Toast.LENGTH_SHORT).show()
            }
            uri
        }.onFailure { e ->
            android.util.Log.e("NovelExcerptHelper", "Failed to save excerpt", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.novel_excerpt_share_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun shareExcerpt(
        context: Context,
        data: NovelExcerptData,
        configuration: NovelExcerptConfiguration,
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = NovelExcerptCardRenderer.render(data, configuration)
            val directory = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(directory, "kototoro_excerpt_${System.currentTimeMillis()}.png")
            file.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", file)

            withContext(Dispatchers.Main) {
                ShareHelper(context).shareImage(uri)
            }
            uri
        }.onFailure { e ->
            android.util.Log.e("NovelExcerptHelper", "Failed to share excerpt", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.novel_excerpt_share_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
