package dev.prism.gallery.ui.viewer

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

data class MediaExif(
    val dateTaken: String = "",
    val location: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0,
    val mimeType: String = "",
    val cameraMake: String = "",
    val cameraModel: String = "",
)

suspend fun readExif(context: Context, uri: Uri, sizeBytes: Long, mimeType: String): MediaExif =
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)

                val latLon = FloatArray(2)
                val hasLocation = exif.getLatLong(latLon)
                val location = if (hasLocation) {
                    reverseGeocode(context, latLon[0].toDouble(), latLon[1].toDouble())
                } else ""

                val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                    ?: ""

                val w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                val h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
                val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""

                MediaExif(
                    dateTaken = dateTime,
                    location = location,
                    width = w,
                    height = h,
                    sizeBytes = sizeBytes,
                    mimeType = mimeType,
                    cameraMake = make,
                    cameraModel = model,
                )
            } ?: MediaExif(sizeBytes = sizeBytes, mimeType = mimeType)
        } catch (_: IOException) {
            MediaExif(sizeBytes = sizeBytes, mimeType = mimeType)
        }
    }

private fun reverseGeocode(context: Context, lat: Double, lon: Double): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(lat, lon, 1)
        if (!addresses.isNullOrEmpty()) {
            val addr = addresses[0]
            listOfNotNull(
                addr.locality,
                addr.adminArea,
                addr.countryName,
            ).joinToString(", ")
        } else {
            "%.4f, %.4f".format(lat, lon)
        }
    } catch (_: Exception) {
        "%.4f, %.4f".format(lat, lon)
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}
