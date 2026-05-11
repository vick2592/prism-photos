package dev.prism.gallery.ui.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaInfoSheet(
    exif: MediaExif,
    displayName: String,
    sheetState: SheetState,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
            )
            HorizontalDivider()
            if (exif.dateTaken.isNotBlank()) {
                InfoRow(icon = Icons.Filled.Schedule, label = "Date", value = exif.dateTaken)
            }
            if (exif.location.isNotBlank()) {
                InfoRow(icon = Icons.Filled.LocationOn, label = "Location", value = exif.location)
            }
            if (exif.width > 0 && exif.height > 0) {
                InfoRow(
                    icon = Icons.Filled.Image,
                    label = "Resolution",
                    value = "${exif.width} × ${exif.height}",
                )
            }
            if (exif.sizeBytes > 0) {
                InfoRow(
                    icon = Icons.Filled.Storage,
                    label = "Size",
                    value = formatBytes(exif.sizeBytes),
                )
            }
            val cameraLabel = listOf(exif.cameraMake, exif.cameraModel)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            if (cameraLabel.isNotBlank()) {
                InfoRow(icon = Icons.Filled.CameraAlt, label = "Camera", value = cameraLabel)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
