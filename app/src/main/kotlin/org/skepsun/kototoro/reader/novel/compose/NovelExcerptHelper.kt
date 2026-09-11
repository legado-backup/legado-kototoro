package org.skepsun.kototoro.reader.novel.compose

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.skepsun.kototoro.BuildConfig
import org.skepsun.kototoro.R
import org.skepsun.kototoro.core.util.ShareHelper
import org.skepsun.kototoro.core.util.ext.toBitmapOrNull
import java.io.File

object NovelExcerptHelper {

    suspend fun loadExcerptBitmap(context: Context, uriString: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (uriString.isNullOrBlank()) return@withContext null
        runCatching {
            val request = ImageRequest.Builder(context)
                .data(uriString)
                .allowHardware(false)
                .build()
            SingletonImageLoader.get(context).execute(request).toBitmapOrNull()
        }.getOrNull() ?: runCatching {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file" || uri.path?.startsWith("/") == true) {
                val file = if (uri.scheme == "file") File(uri.path.orEmpty()) else File(uriString)
                if (file.exists()) {
                    android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                } else null
            } else if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream)
                }
            } else null
        }.getOrNull()
    }

    suspend fun saveExcerptToGallery(
        context: Context,
        data: NovelExcerptData,
        configuration: NovelExcerptConfiguration,
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val resolvedData = if (data.imageBitmap == null && !data.imageUri.isNullOrBlank()) {
                val bmp = loadExcerptBitmap(context, data.imageUri)
                if (bmp != null) data.copy(imageBitmap = bmp) else data
            } else data
            val bitmap = NovelExcerptCardRenderer.render(resolvedData, configuration, context = context)
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
            val resolvedData = if (data.imageBitmap == null && !data.imageUri.isNullOrBlank()) {
                val bmp = loadExcerptBitmap(context, data.imageUri)
                if (bmp != null) data.copy(imageBitmap = bmp) else data
            } else data
            val bitmap = NovelExcerptCardRenderer.render(resolvedData, configuration, context = context)
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
