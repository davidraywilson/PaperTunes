package moe.rukamori.archivetune.eink.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.LibraryAddCheck
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.Headphones
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.random.Random
import androidx.media3.common.Player
import androidx.navigation.NavController
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.paperapps.paperui.components.PaperLazyColumn
import kotlinx.coroutines.delay
import com.mudita.mmd.components.text.TextMMD
import com.paperapps.paperui.components.DashedDivider
import moe.rukamori.archivetune.LocalPlayerConnection
import moe.rukamori.archivetune.extensions.togglePlayPause
import moe.rukamori.archivetune.utils.makeTimeString
import moe.rukamori.archivetune.eink.components.EinkAddToPlaylistDialog
import moe.rukamori.archivetune.eink.components.OutlinedPause

import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.paperapps.paperui.components.AppbarAction
import com.paperapps.paperui.components.ApplicationBar
import com.paperapps.paperui.components.PanoramaHeader
import com.paperapps.paperui.components.PanoramaPager
import com.paperapps.paperui.components.PaperProgressBar
import moe.rukamori.archivetune.LocalDownloadUtil
import moe.rukamori.archivetune.extensions.metadata
import moe.rukamori.archivetune.models.MediaMetadata
import moe.rukamori.archivetune.playback.ExoDownloadService
import com.paperapps.paperui.components.AppbarMenuItem
import moe.rukamori.archivetune.eink.einkYouTubeArtistDetailsRoute
import moe.rukamori.archivetune.eink.EinkScreen
import moe.rukamori.archivetune.playback.queues.YouTubeQueue

@Composable
fun EinkNowPlayingScreen(navController: NavController) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val shuffleEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "").collectAsState(initial = null)
    
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    val titles = listOf("Now Playing", "Coming Up")
    val pagerState = rememberPagerState(pageCount = { titles.size })

    LaunchedEffect(playerConnection, isPlaying) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it > 0L }
                ?: ((mediaMetadata?.duration ?: 0) * 1000L)
            sliderPosition = if (duration > 0L) position.toFloat() / duration else 0f
            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        PanoramaHeader(
            pagerState = pagerState,
            titles = titles,
            coroutineScope = coroutineScope,
            modifier = Modifier.fillMaxWidth()
        )

        PanoramaPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            if (page == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    EinkVisualizer(
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .padding(vertical = 4.dp)
                    )

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
                            fontWeight = FontWeight.Bold,
                            lineHeight = 48.sp,
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
                        PaperProgressBar(
                            progress = sliderPosition.coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                            onProgressChange = { value ->
                                sliderPosition = value
                                if (duration > 0) {
                                    val newPosition = (value * duration).toLong().coerceIn(0L, duration)
                                    player.seekTo(newPosition)
                                }
                            }
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
                        IconButton(
                            onClick = { playerConnection.seekToPrevious() },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SkipPrevious,
                                contentDescription = "Previous Song",
                                modifier = Modifier.size(46.dp),
                            )
                        }

                        IconButton(
                            onClick = { player.togglePlayPause() },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) OutlinedPause else Icons.Outlined.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(46.dp),
                            )
                        }

                        IconButton(
                            onClick = { playerConnection.seekToNext() },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SkipNext,
                                contentDescription = "Next Song",
                                modifier = Modifier.size(46.dp),
                            )
                        }
                    }
                }
            } else {
                PaperLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    refreshKey = queueWindows,
                ) {
                    itemsIndexed(queueWindows) { index, window ->
                        val metadata = window.mediaItem.metadata
                        if (metadata != null) {
                            EinkQueueRow(
                                metadata = metadata,
                                isCurrentlyPlaying = index == currentWindowIndex,
                                onClick = {
                                    player.seekToDefaultPosition(index)
                                    player.play()
                                },
                                showDivider = index != queueWindows.lastIndex,
                            )
                        }
                    }
                }
            }
        }
        
        val downloadIcon = when (download?.state) {
            Download.STATE_COMPLETED -> Icons.Outlined.DownloadDone
            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> Icons.Outlined.Downloading
            else -> Icons.Outlined.Download
        }
        
        val downloadLabel = when (download?.state) {
            Download.STATE_COMPLETED -> "Downloaded"
            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> "Downloading"
            else -> "Download"
        }

        ApplicationBar(
            actions = listOf(
                AppbarAction(
                    icon = Icons.Outlined.Shuffle,
                    label = "Shuffle",
                    isActive = shuffleEnabled,
                    onClick = { player.shuffleModeEnabled = !player.shuffleModeEnabled }
                ),
                AppbarAction(
                    icon = when (repeatMode) {
                        Player.REPEAT_MODE_ALL -> Icons.Outlined.Repeat
                        Player.REPEAT_MODE_ONE -> Icons.Outlined.RepeatOne
                        else -> Icons.Outlined.Repeat
                    },
                    label = "Repeat",
                    isActive = repeatMode != Player.REPEAT_MODE_OFF,
                    onClick = {
                        player.repeatMode = when (player.repeatMode) {
                            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                            else -> Player.REPEAT_MODE_OFF
                        }
                    }
                ),
                AppbarAction(
                    icon = downloadIcon,
                    label = downloadLabel,
                    onClick = {
                        val state = download?.state
                        if (state == Download.STATE_COMPLETED || state == Download.STATE_QUEUED || state == Download.STATE_DOWNLOADING) {
                            mediaMetadata?.let { metadata ->
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    metadata.id,
                                    false,
                                )
                            }
                        } else {
                            mediaMetadata?.let { metadata ->
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
                        }
                    }
                )
            ),
            menuItems = buildList {
                add(
                    AppbarMenuItem(
                        label = "Add to Playlist",
                        onClick = {
                            if (mediaMetadata != null) {
                                showAddToPlaylistDialog = true
                            }
                        }
                    )
                )
                val artistId = mediaMetadata?.artists?.firstOrNull()?.id
                if (artistId != null) {
                    add(
                        AppbarMenuItem(
                            label = "View Artist",
                            onClick = {
                                navController.navigate(einkYouTubeArtistDetailsRoute(artistId))
                            }
                        )
                    )
                }
                val albumId = mediaMetadata?.album?.id
                if (albumId != null) {
                    add(
                        AppbarMenuItem(
                            label = "View Album",
                            onClick = {
                                navController.navigate("${EinkScreen.AlbumDetails.route}/${albumId}")
                            }
                        )
                    )
                }
                if (mediaMetadata != null) {
                    add(
                        AppbarMenuItem(
                            label = "Start Radio",
                            onClick = {
                                playerConnection.playQueue(YouTubeQueue.radio(mediaMetadata!!))
                            }
                        )
                    )
                }
            },
            pagerState = pagerState,
            onBack = { navController.navigateUp() }
        )
    }

    if (showAddToPlaylistDialog && mediaMetadata != null) {
        EinkAddToPlaylistDialog(
            mediaMetadata = mediaMetadata!!,
            onDismiss = { showAddToPlaylistDialog = false }
        )
    }
}

@Composable
private fun EinkQueueRow(
    metadata: MediaMetadata,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isCurrentlyPlaying) {
                Icon(
                    imageVector = Icons.Outlined.Headphones,
                    contentDescription = "Now playing",
                    modifier = Modifier
                        .size(24.dp)
                        .padding(start = 4.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                TextMMD(
                    text = metadata.title.orEmpty(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                val artist = metadata.artists.joinToString(", ") { it.name }
                val album = metadata.album?.title?.takeIf { it.isNotBlank() }
                val duration = metadata.duration.takeIf { it > 0 }?.let { makeTimeString(it * 1000L) }
                val subtitle = buildString {
                    if (artist.isNotBlank()) append(artist)
                    if (!album.isNullOrBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(album)
                    }
                    if (!duration.isNullOrBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(duration)
                    }
                }
                TextMMD(
                    text = subtitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (showDivider) DashedDivider(thickness = 1.dp)
    }
}

@Composable
fun EinkVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    maxBlocksPerBar: Int = 8
) {
    val targetAmplitudes = remember { FloatArray(barCount) { 0.1f } }
    val currentAmplitudes = remember { FloatArray(barCount) { 0.1f } }
    
    var tick by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                for (i in 0 until barCount) {
                    if (Random.nextFloat() < 0.5f || targetAmplitudes[i] <= 0.1f) {
                        targetAmplitudes[i] = Random.nextFloat()
                    }
                    currentAmplitudes[i] += (targetAmplitudes[i] - currentAmplitudes[i]) * 0.6f
                }
                tick = System.currentTimeMillis()
                delay(200)
            }
        } else {
            while (currentAmplitudes.any { it > 0.15f }) {
                for (i in 0 until barCount) {
                    currentAmplitudes[i] = (currentAmplitudes[i] * 0.5f).coerceAtLeast(0.1f)
                }
                tick = System.currentTimeMillis()
                delay(200)
            }
            for (i in 0 until barCount) {
                currentAmplitudes[i] = 0.1f
            }
            tick = System.currentTimeMillis()
        }
    }
    
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier) {
        val t = tick 
        val width = size.width
        val height = size.height
        
        val barSpacing = 2.dp.toPx()
        val blockSpacing = 1.dp.toPx()
        
        val totalBarWidth = (width - barSpacing * (barCount - 1)) / barCount
        val barWidth = totalBarWidth.coerceAtLeast(1f)
        
        val totalBlockHeight = (height - blockSpacing * (maxBlocksPerBar - 1)) / maxBlocksPerBar
        val blockHeight = totalBlockHeight.coerceAtLeast(1f)
        
        for (i in 0 until barCount) {
            val amplitude = currentAmplitudes[i]
            val blocksToShow = (amplitude * maxBlocksPerBar).roundToInt().coerceIn(1, maxBlocksPerBar)
            
            val x = i * (barWidth + barSpacing)
            
            for (b in 0 until blocksToShow) {
                val y = b * (blockHeight + blockSpacing)
                drawRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, blockHeight)
                )
            }
        }
    }
}
