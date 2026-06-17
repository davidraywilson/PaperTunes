/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.checkbox.CheckboxMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.constants.InnerTubeCookieKey
import moe.rukamori.archivetune.db.entities.PlaylistEntity
import moe.rukamori.archivetune.extensions.isSyncEnabled
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.utils.rememberPreference
import java.time.LocalDateTime
import java.util.logging.Logger
// For TextField, we use a basic string field. Since we might not have TextFieldMMD, we can use BasicTextField
// or standard Material3 TextField styled for E-ink. Let's use Material3 OutlinedTextField, which is okay, 
// but wait, Eink components avoid standard Material3 when possible.
// Actually, let's use standard MMD components if available, but if not we can use BasicTextField.
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border

@Composable
fun EinkCreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String = "",
    allowSyncing: Boolean = true,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var playlistName by remember { mutableStateOf(initialTextFieldValue) }
    var syncedPlaylist by remember { mutableStateOf(false) }

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isSignedIn = innerTubeCookie.isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            TextMMD(
                text = "Create Playlist",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            BasicTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 18.sp,
                    color = Color.Black
                ),
                cursorBrush = SolidColor(Color.Black),
                decorationBox = { innerTextField ->
                    if (playlistName.isEmpty()) {
                        TextMMD("Playlist name", color = Color.Gray, fontSize = 18.sp)
                    }
                    innerTextField()
                }
            )

            if (allowSyncing) {
                Spacer(modifier = Modifier.height(16.dp))
                val isYtmSyncEnabled = context.isSyncEnabled()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CheckboxMMD(
                        checked = syncedPlaylist,
                        onCheckedChange = {
                            if (syncedPlaylist) {
                                syncedPlaylist = false
                                return@CheckboxMMD
                            }
                            if (!isSignedIn) {
                                Toast.makeText(context, context.getString(R.string.not_logged_in_youtube), Toast.LENGTH_SHORT).show()
                                return@CheckboxMMD
                            }
                            if (!isYtmSyncEnabled) {
                                Toast.makeText(context, context.getString(R.string.sync_disabled), Toast.LENGTH_SHORT).show()
                                return@CheckboxMMD
                            }
                            syncedPlaylist = true
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        TextMMD("Sync playlist", fontWeight = FontWeight.SemiBold)
                        TextMMD(
                            text = when {
                                !isSignedIn -> "Sign in to YouTube Music to sync"
                                !isYtmSyncEnabled -> "Syncing is disabled in settings"
                                else -> "Playlist will sync with YouTube Music"
                            },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButtonMMD(onClick = onDismiss) {
                    TextMMD("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                ButtonMMD(
                    onClick = {
                        val name = playlistName.trim()
                        if (name.isNotEmpty()) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val browseId = if (syncedPlaylist && isSignedIn) {
                                    YouTube.createPlaylist(name).getOrNull()
                                } else if (syncedPlaylist) {
                                    Logger.getLogger("CreatePlaylistDialog").warning("Not signed in")
                                    return@launch
                                } else null

                                database.withTransaction {
                                    insert(
                                        PlaylistEntity(
                                            name = name,
                                            browseId = browseId,
                                            bookmarkedAt = LocalDateTime.now(),
                                            isEditable = true,
                                        ),
                                    )
                                }
                                onDismiss()
                            }
                        }
                    }
                ) {
                    TextMMD("Create")
                }
            }
        }
    }
}

@Composable
fun EinkEditPlaylistDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var playlistName by remember { mutableStateOf(initialName) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            TextMMD(
                text = "Rename Playlist",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            BasicTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 18.sp,
                    color = Color.Black
                ),
                cursorBrush = SolidColor(Color.Black),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButtonMMD(onClick = onDismiss) {
                    TextMMD("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                ButtonMMD(
                    onClick = {
                        val name = playlistName.trim()
                        if (name.isNotEmpty() && name != initialName) {
                            onSave(name)
                        }
                        onDismiss()
                    }
                ) {
                    TextMMD("Save")
                }
            }
        }
    }
}
