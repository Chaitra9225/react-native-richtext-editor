package com.richtext.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.webkit.MimeTypeMap
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.WritableMap
import org.json.JSONObject
import java.net.URL

class MediaAttachmentSupport(
    private val context: Context,
    private val density: Float,
    private val placeholderChar: Char,
    private val getLineSpacingMultiplier: () -> Float,
    private val getTargetWidthPx: () -> Int,
    private val editableProvider: () -> Editable?,
    private val runOnUiThread: ((() -> Unit) -> Unit),
    private val onMediaSpansUpdated: () -> Unit
) {

    private val mediaBitmapCache = mutableMapOf<String, Bitmap>()
    private val loadingRemoteUris = mutableSetOf<String>()

    // Block Data to MediaAttachmentData parsing
    fun parseMediaData(block: Map<String, Any>): MediaAttachmentData? {
        val blockType = block["type"] as? String ?: return null
        if (blockType != "mediaAttachment") return null

        val mediaInfo = block["mediaAttachment"] as? Map<*, *>
        val uri = mediaInfo?.get("uri") as? String ?: ""
        val sourceUri = mediaInfo?.get("sourceUri") as? String ?: uri
        return MediaAttachmentData(
            kind = mediaInfo?.get("kind") as? String ?: "image",
            uri = uri,
            sourceUri = sourceUri,
            fileName = mediaInfo?.get("fileName") as? String,
            extension = mediaInfo?.get("extension") as? String,
            contentType = mediaInfo?.get("contentType") as? String,
            fileSize = (mediaInfo?.get("fileSize") as? Number)?.toLong(),
            widthDp = (mediaInfo?.get("width") as? Number)?.toInt() ?: 100,
            heightDp = (mediaInfo?.get("height") as? Number)?.toInt() ?: 100,
            alt = mediaInfo?.get("alt") as? String ?: ""
        )
    }

    fun createMediaAttachmentSpan(mediaData: MediaAttachmentData): MediaAttachmentSpan {
        val targetWidthPx = getTargetWidthPx().coerceAtLeast(1)
        val loaded = loadBitmapForMedia(mediaData, targetWidthPx)
        return if (loaded != null) {
            val (bitmap, updatedData) = loaded
            MediaAttachmentSpan(updatedData, density, bitmap, getLineSpacingMultiplier())
        } else {
            val fallbackData = normalizeMediaDimensions(mediaData, targetWidthPx)
            MediaAttachmentSpan(fallbackData, density, null, getLineSpacingMultiplier())
        }
    }

    fun findMediaAttachmentSpan(spannable: Spanned, lineStart: Int, lineEnd: Int): MediaAttachmentSpan? {
        val safeEnd = lineEnd.coerceAtLeast(lineStart + 1)
        return spannable.getSpans(lineStart, safeEnd, MediaAttachmentSpan::class.java)
            .firstOrNull { span ->
                val spanStart = spannable.getSpanStart(span)
                val spanEnd = spannable.getSpanEnd(span)
                spanStart < lineEnd && spanEnd > lineStart
            }
    }

    fun isMediaLine(line: String): Boolean {
        return line.replace(placeholderChar.toString(), "").trim().isEmpty()
    }

    fun createWritableMediaBlock(mediaData: MediaAttachmentData): WritableMap {
        val block = Arguments.createMap()
        block.putString("type", "mediaAttachment")
        block.putString("text", "")
        block.putArray("styles", Arguments.createArray())

        val mediaMap = Arguments.createMap()
        mediaMap.putString("kind", mediaData.kind)
        mediaMap.putString("uri", mediaData.uri)
        mediaMap.putString("sourceUri", mediaData.sourceUri ?: mediaData.uri)
        mediaData.fileName?.let { mediaMap.putString("fileName", it) }
        mediaData.extension?.let { mediaMap.putString("extension", it) }
        mediaData.contentType?.let { mediaMap.putString("contentType", it) }
        mediaData.fileSize?.let { mediaMap.putDouble("fileSize", it.toDouble()) }
        mediaMap.putInt("width", mediaData.widthDp)
        mediaMap.putInt("height", mediaData.heightDp)
        mediaMap.putString("alt", mediaData.alt)
        block.putMap("mediaAttachment", mediaMap)

        return block
    }

    fun createJsonMediaBlock(mediaData: MediaAttachmentData): JSONObject {
        val block = JSONObject()
        block.put("type", "mediaAttachment")
        block.put("text", "")
        block.put("styles", org.json.JSONArray())

        val mediaObj = JSONObject()
        mediaObj.put("kind", mediaData.kind)
        mediaObj.put("uri", mediaData.uri)
        mediaObj.put("sourceUri", mediaData.sourceUri ?: mediaData.uri)
        mediaData.fileName?.let { mediaObj.put("fileName", it) }
        mediaData.extension?.let { mediaObj.put("extension", it) }
        mediaData.contentType?.let { mediaObj.put("contentType", it) }
        mediaData.fileSize?.let { mediaObj.put("fileSize", it) }
        mediaObj.put("width", mediaData.widthDp)
        mediaObj.put("height", mediaData.heightDp)
        mediaObj.put("alt", mediaData.alt)
        block.put("mediaAttachment", mediaObj)

        return block
    }

    fun appendMediaBlock(
        spannable: SpannableStringBuilder,
        currentOffset: Int,
        mediaData: MediaAttachmentData,
        appendTrailingNewline: Boolean
    ): Int {
        var offset = currentOffset
        val spanStart = offset
        spannable.append(placeholderChar)
        offset += 1
        spannable.setSpan(
            createMediaAttachmentSpan(mediaData),
            spanStart,
            spanStart + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        if (appendTrailingNewline) {
            spannable.append("\n")
            offset += 1
        }

        return offset
    }

    fun insertMediaAttachmentBlock(editable: Editable, insertPos: Int, uri: String): Int {
        val mediaData = createMediaDataForUri(uri)
        return insertMediaAttachmentBlock(editable, insertPos, mediaData)
    }

    fun insertMediaAttachmentBlock(editable: Editable, insertPos: Int, mediaData: MediaAttachmentData): Int {
        var mutableInsertPos = insertPos.coerceIn(0, editable.length)

        if (mutableInsertPos > 0 && editable[mutableInsertPos - 1] != '\n') {
            editable.insert(mutableInsertPos, "\n")
            mutableInsertPos += 1
        }

        val spanStart = mutableInsertPos
        editable.insert(spanStart, placeholderChar.toString())
        editable.setSpan(
            createMediaAttachmentSpan(mediaData),
            spanStart,
            spanStart + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        var nextPos = spanStart + 1
        val hasFollowingText = nextPos < editable.length
        if (hasFollowingText && editable[nextPos] != '\n') {
            editable.insert(nextPos, "\n")
            nextPos += 1
        }

        return nextPos.coerceAtMost(editable.length)
    }

    fun createMediaDataForUri(uri: String): MediaAttachmentData {
        val inferredMeta = inferMediaMetadata(uri)
        return MediaAttachmentData(
            kind = "image",
            uri = uri,
            sourceUri = uri,
            fileName = inferredMeta.fileName,
            extension = inferredMeta.extension,
            contentType = inferredMeta.contentType,
            fileSize = inferredMeta.fileSize,
            widthDp = 100,
            heightDp = 100,
            alt = "Selected image"
        )
    }

    private data class InferredMediaMetadata(
        val fileName: String?,
        val extension: String?,
        val contentType: String?,
        val fileSize: Long?
    )

    private fun inferMediaMetadata(uriString: String): InferredMediaMetadata {
        val parsed = runCatching { Uri.parse(uriString) }.getOrNull()
            ?: return InferredMediaMetadata(null, null, null, null)

        val extension = runCatching {
            MimeTypeMap.getFileExtensionFromUrl(parsed.toString())
        }.getOrNull()?.takeIf { it.isNotBlank() }?.lowercase()

        val contentType = runCatching {
            context.contentResolver.getType(parsed)
        }.getOrNull() ?: extension?.let {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
        }

        var fileName: String? = null
        var fileSize: Long? = null

        if (parsed.scheme == "content") {
            runCatching {
                context.contentResolver.query(parsed, null, null, null, null)
            }.getOrNull()?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex)
                    }
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        }

        if (fileName.isNullOrBlank()) {
            fileName = parsed.lastPathSegment
        }

        val resolvedExtension = extension ?: fileName
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() }
            ?.lowercase()

        return InferredMediaMetadata(fileName, resolvedExtension, contentType, fileSize)
    }

    private fun normalizeMediaDimensions(mediaData: MediaAttachmentData, targetWidthPx: Int): MediaAttachmentData {
        val targetWidthDp = (targetWidthPx / density).toInt().coerceAtLeast(1)
        val fallbackHeightDp = if (mediaData.widthDp > 0 && mediaData.heightDp > 0) {
            ((mediaData.heightDp.toFloat() / mediaData.widthDp.toFloat()) * targetWidthDp).toInt().coerceAtLeast(1)
        } else {
            targetWidthDp
        }

        return mediaData.copy(
            widthDp = targetWidthDp,
            heightDp = fallbackHeightDp
        )
    }

    private fun buildScaledBitmapWithAspect(bitmap: Bitmap, targetWidthPx: Int): Pair<Bitmap, Int> {
        val safeTargetWidthPx = targetWidthPx.coerceAtLeast(1)
        val aspectRatio = if (bitmap.width > 0) {
            bitmap.height.toFloat() / bitmap.width.toFloat()
        } else {
            1f
        }
        val targetHeightPx = (safeTargetWidthPx * aspectRatio).toInt().coerceAtLeast(1)

        val scaled = if (bitmap.width == safeTargetWidthPx && bitmap.height == targetHeightPx) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, safeTargetWidthPx, targetHeightPx, true)
        }

        return Pair(scaled, targetHeightPx)
    }

    private fun hasExplicitDimensions(mediaData: MediaAttachmentData): Boolean {
        return mediaData.widthDp > 0 && mediaData.heightDp > 0
    }

    private fun scaleToExplicitDimensions(bitmap: Bitmap, mediaData: MediaAttachmentData, targetWidthPx: Int): Pair<Bitmap, MediaAttachmentData> {
        val explicitWidthPx = (mediaData.widthDp * density).toInt().coerceAtMost(targetWidthPx).coerceAtLeast(1)
        val scale = explicitWidthPx.toFloat() / (mediaData.widthDp * density).coerceAtLeast(1f)
        val explicitHeightPx = (mediaData.heightDp * density * scale).toInt().coerceAtLeast(1)
        val scaled = if (bitmap.width == explicitWidthPx && bitmap.height == explicitHeightPx) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, explicitWidthPx, explicitHeightPx, true)
        }
        val updatedData = mediaData.copy(
            widthDp = (explicitWidthPx / density).toInt().coerceAtLeast(1),
            heightDp = (explicitHeightPx / density).toInt().coerceAtLeast(1)
        )
        return Pair(scaled, updatedData)
    }

    private fun loadBitmapForMedia(mediaData: MediaAttachmentData, targetWidthPx: Int): Pair<Bitmap, MediaAttachmentData>? {
        if (mediaData.uri.isBlank() || mediaData.uri == "red-box-placeholder") return null

        mediaBitmapCache[mediaData.uri]?.let { cached ->
            if (hasExplicitDimensions(mediaData)) {
                return scaleToExplicitDimensions(cached, mediaData, targetWidthPx)
            }
            val (scaledBitmap, targetHeightPx) = buildScaledBitmapWithAspect(cached, targetWidthPx)
            val updatedData = mediaData.copy(
                widthDp = (targetWidthPx / density).toInt().coerceAtLeast(1),
                heightDp = (targetHeightPx / density).toInt().coerceAtLeast(1)
            )
            return Pair(scaledBitmap, updatedData)
        }

        return try {
            val uri = Uri.parse(mediaData.uri)

            when (uri.scheme?.lowercase()) {
                "http", "https" -> {
                    loadRemoteBitmapAsync(mediaData, targetWidthPx)
                    null
                }
                else -> {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }?.let { original ->
                        mediaBitmapCache[mediaData.uri] = original
                        if (hasExplicitDimensions(mediaData)) {
                            return scaleToExplicitDimensions(original, mediaData, targetWidthPx)
                        }
                        val (scaledBitmap, targetHeightPx) = buildScaledBitmapWithAspect(original, targetWidthPx)
                        val updatedData = mediaData.copy(
                            widthDp = (targetWidthPx / density).toInt().coerceAtLeast(1),
                            heightDp = (targetHeightPx / density).toInt().coerceAtLeast(1)
                        )
                        Pair(scaledBitmap, updatedData)
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun loadRemoteBitmapAsync(mediaData: MediaAttachmentData, targetWidthPx: Int) {
        val uriString = mediaData.uri
        synchronized(loadingRemoteUris) {
            if (loadingRemoteUris.contains(uriString)) return
            loadingRemoteUris.add(uriString)
        }

        Thread {
            try {
                val loaded = URL(uriString).openStream().use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                } ?: return@Thread

                mediaBitmapCache[uriString] = loaded
                val result = if (hasExplicitDimensions(mediaData)) {
                    scaleToExplicitDimensions(loaded, mediaData, targetWidthPx)
                } else {
                    val (scaledBitmap, targetHeightPx) = buildScaledBitmapWithAspect(loaded, targetWidthPx)
                    Pair(scaledBitmap, mediaData.copy(
                        widthDp = (targetWidthPx / density).toInt().coerceAtLeast(1),
                        heightDp = (targetHeightPx / density).toInt().coerceAtLeast(1)
                    ))
                }
                val (scaledBitmap, updatedData) = result

                runOnUiThread {
                    synchronized(loadingRemoteUris) {
                        loadingRemoteUris.remove(uriString)
                    }
                    refreshMediaSpansForUri(uriString, scaledBitmap, updatedData)
                }
            } catch (_: Exception) {
                runOnUiThread {
                    synchronized(loadingRemoteUris) {
                        loadingRemoteUris.remove(uriString)
                    }
                }
            }
        }.start()
    }

    private fun refreshMediaSpansForUri(uriString: String, bitmap: Bitmap, updatedData: MediaAttachmentData) {
        val editable = editableProvider() ?: return
        val spans = editable.getSpans(0, editable.length, MediaAttachmentSpan::class.java)
        var updated = false

        spans.forEach { span ->
            val data = span.toMediaAttachmentData()
            if (data.uri == uriString) {
                val start = editable.getSpanStart(span)
                val end = editable.getSpanEnd(span)
                val flags = editable.getSpanFlags(span)
                editable.removeSpan(span)
                editable.setSpan(
                    MediaAttachmentSpan(updatedData, density, bitmap, getLineSpacingMultiplier()),
                    start,
                    end,
                    flags
                )
                updated = true
            }
        }

        if (updated) {
            onMediaSpansUpdated()
        }
    }
}
