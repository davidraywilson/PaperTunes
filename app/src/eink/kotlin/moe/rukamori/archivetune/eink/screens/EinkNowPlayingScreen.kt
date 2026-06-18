package moe.rukamori.archivetune.eink.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.LibraryAddCheck
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.navigation.NavController
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.slider.SliderMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import kotlinx.coroutines.delay
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.extensions.togglePlayPause
import moe.rukamori.archivetune.utils.makeTimeString
import moe.rukamori.archivetune.eink.components.EinkAddToPlaylistDialog

import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import moe.rukamori.archivetune.LocalDatabase
import moe.rukamori.archivetune.LocalDownloadUtil
import moe.rukamori.archivetune.playback.ExoDownloadService

@Composable
fun EinkNowPlayingScreen(navController: NavController) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val database = LocalDatabase.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val shuffleEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "").collectAsState(initial = null)

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(playerConnection, isPlaying) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it > 0L }
                ?: ((mediaMetadata?.duration ?: 0) * 1000L)
            if (!isSeeking) {
                sliderPosition = if (duration > 0L) position.toFloat() / duration else 0f
            }
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = mediaMetadata?.title.orEmpty(),
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mediaMetadata?.artists?.joinToString(", ") { it.name }.orEmpty(),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            val albumName = mediaMetadata?.album?.title
            if (!albumName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = albumName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            SliderMMD(
                modifier = Modifier.fillMaxWidth(),
                value = sliderPosition.coerceIn(0f, 1f),
                onValueChange = { value ->
                    isSeeking = true
                    sliderPosition = value
                    if (duration > 0) {
                        val newPosition = (value * duration).toLong().coerceIn(0L, duration)
                        player.seekTo(newPosition)
                    }
                    isSeeking = false
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = makeTimeString(position),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = makeTimeString(duration),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonMMD(
                onClick = { playerConnection.seekToPrevious() },
                modifier = Modifier.size(72.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )

            ) {
                Icon(
                    imageVector = Icons.Outlined.SkipPrevious,
                    modifier = Modifier.size(46.dp),
                    contentDescription = "Previous Song",
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }

            IconButton(
                onClick = { player.togglePlayPause() },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(46.dp),
                )
            }

            ButtonMMD(
                onClick = { playerConnection.seekToNext() },
                modifier = Modifier.size(72.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.SkipNext,
                    modifier = Modifier.size(46.dp),
                    contentDescription = "Next Song",
                    tint = MaterialTheme.colorScheme.onSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom row for secondary actions (e.g. shuffle, repeat, add to playlist / library)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Add to library not implemented in Eink mode right now, just match visual */ }) {
                Icon(
                    imageVector = Icons.Outlined.LibraryAdd,
                    contentDescription = "Add to library",
                )
            }

            IconButton(onClick = { 
                if (mediaMetadata != null) {
                    showAddToPlaylistDialog = true
                }
            }) {
                Icon(
                    imageVector = Icons.Outlined.PlaylistAdd,
                    contentDescription = "Add to playlist",
                )
            }

            when (download?.state) {
                Download.STATE_COMPLETED -> {
                    IconButton(onClick = {
                        mediaMetadata?.let { metadata ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                metadata.id,
                                false,
                            )
                        }
                    }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = moe.rukamori.archivetune.R.drawable.offline),
                            contentDescription = "Downloaded",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                    IconButton(onClick = {
                        mediaMetadata?.let { metadata ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                metadata.id,
                                false,
                            )
                        }
                    }) {
                        CircularProgressIndicatorMMD(
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                else -> {
                    IconButton(onClick = {
                        mediaMetadata?.let { metadata ->
                            database.transaction {
                                insert(metadata)
                            }
                            val downloadRequest = DownloadRequest
                                .Builder(metadata.id, metadata.id.toUri())
                                .setCustomCacheKey(metadata.id)
                                .setData((metadata.title ?: "").toByteArray())
                                .build()
                            DownloadService.sendAddDownload(
                                context,
                                ExoDownloadService::class.java,
                                downloadRequest,
                                false,
                            )
                        }
                    }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = moe.rukamori.archivetune.R.drawable.download),
                            contentDescription = "Download song",
                        )
                    }
                }
            }

            IconButton(onClick = { player.shuffleModeEnabled = !player.shuffleModeEnabled }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Shuffle,
                        contentDescription = "Shuffle queue",
                    )
                    if (shuffleEnabled) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
            }

            IconButton(onClick = {
                player.repeatMode = when (player.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            }) {
                val (icon, description, isActive) = when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> Triple(Icons.Outlined.Repeat, "Repeat off", false)
                    Player.REPEAT_MODE_ALL -> Triple(Icons.Outlined.Repeat, "Repeat queue", true)
                    else -> Triple(Icons.Outlined.RepeatOne, "Repeat current song", true)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = icon,
                        contentDescription = description,
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
            }
        }
    }

    if (showAddToPlaylistDialog && mediaMetadata != null) {
        EinkAddToPlaylistDialog(
            songId = mediaMetadata!!.id,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }
}
