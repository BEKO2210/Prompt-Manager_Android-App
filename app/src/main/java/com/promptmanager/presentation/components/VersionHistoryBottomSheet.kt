package com.promptmanager.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.promptmanager.data.database.PromptEntity
import com.promptmanager.data.repository.PromptRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * BottomSheet zur Anzeige der Versions-Historie eines Prompts.
 *
 * Features:
 * - Liste aller Versionen (chronologisch)
 * - Klick → Version ansehen
 * - Aktuell angezeigte Version markiert
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionHistoryBottomSheet(
    promptId: Long,
    repository: PromptRepository,
    onDismiss: () -> Unit,
    onVersionSelected: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var versions by remember { mutableStateOf<List<PromptEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(promptId) {
        scope.launch {
            repository.getPromptVersions(promptId).collect { versionsList ->
                versions = versionsList.sortedByDescending { it.createdAt }
                isLoading = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Header
            Text(
                text = "Versions-Historie",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Text(
                text = "${versions.size} Versionen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (versions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keine Versionen gefunden",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(versions) { version ->
                        VersionItem(
                            version = version,
                            isCurrentVersion = version.id == promptId,
                            onClick = {
                                onVersionSelected(version.id)
                                onDismiss()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Close Button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text("Schließen")
            }
        }
    }
}

@Composable
private fun VersionItem(
    version: PromptEntity,
    isCurrentVersion: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = if (isCurrentVersion) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Version ${version.version}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isCurrentVersion) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (isCurrentVersion) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Aktuelle Version",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatDate(version.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrentVersion) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!version.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = version.description!!,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        color = if (isCurrentVersion) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${version.usageCount}× genutzt",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrentVersion) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
