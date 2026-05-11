package dev.prism.gallery.domain.model

import android.net.Uri

data class Album(
    val id: String,          // MediaStore BUCKET_ID
    val name: String,        // Folder name shown in UI
    val coverUri: Uri,       // Most recent item used as cover art
    val count: Int,          // Total number of items
    val dateModified: Long,  // Most recent item's dateTaken for sorting
)
