package dev.prism.gallery.data.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateTaken: Long,
    val dateModified: Long,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val size: Long,
    val bucketId: String,
    val bucketName: String,
    val duration: Long = 0L,       // milliseconds; 0 for images
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isImage: Boolean get() = mimeType.startsWith("image/")
}
