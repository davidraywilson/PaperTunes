
package moe.rukamori.archivetune.eink.screens
import kotlinx.coroutines.launch


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.hilt.navigation.compose.hiltViewModel
import moe.rukamori.archivetune.canvas.CanvasPlaybackRequest
import moe.rukamori.archivetune.canvas.CanvasSource
import moe.rukamori.archivetune.viewmodels.CanvasPlaybackState
import moe.rukamori.archivetune.viewmodels.CanvasPlaybackViewModel
import moe.rukamori.archivetune.ui.player.CanvasArtworkPlayer
import java.util.Locale
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircleOutline
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
import moe.rukamori.archivetune.extensions.toggleRepeatMode
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
fun EinkNowPlayingScreen(navController: NavController, canvasViewModel: CanvasPlaybackViewModel = hiltViewModel()) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val player = playerConnection.player
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val database = moe.rukamori.archivetune.LocalDatabase.current
    val librarySong by database.song(mediaMetadata?.id ?: "").collectAsState(initial = null)
    val isLibrarySong = librarySong?.song?.inLibrary != null
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val shuffleEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "").collectAsState(initial = null)
    val isDownloaded = download?.state == Download.STATE_COMPLETED
    val isPlayerBuffering = playbackState == androidx.media3.common.Player.STATE_BUFFERING
    var isBuffering by remember { mutableStateOf(false) }
    LaunchedEffect(isPlayerBuffering, isDownloaded) {
        if (isPlayerBuffering && !isDownloaded) {
            delay(400)
            isBuffering = true
        } else {
            isBuffering = false
        }
    }
    
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    val titles = listOf("Now Playing", "Coming Up")
    val pagerState = rememberPagerState(pageCount = { titles.size })

    val canvasRequest by remember(mediaMetadata) {
        androidx.compose.runtime.derivedStateOf {
            val metadata = mediaMetadata ?: return@derivedStateOf null
            val country = Locale.getDefault().country
            CanvasPlaybackRequest(
                mediaId = metadata.id,
                title = metadata.title,
                artist = metadata.artists.firstOrNull()?.name.orEmpty(),
                storefront = if (country.length == 2) country.lowercase(Locale.ROOT) else "us",
                requireVertical = false,
            )
        }
    }
    LaunchedEffect(canvasViewModel, canvasRequest) {
        canvasViewModel.setRequest(canvasRequest)
    }
    androidx.compose.runtime.DisposableEffect(canvasViewModel) {
        onDispose { canvasViewModel.setRequest(null) }
    }
    val canvasState by canvasViewModel.state.collectAsState()
    val canvasVideo = (canvasState as? CanvasPlaybackState.Success)?.video
    var isCanvasReady by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(canvasVideo) {
        if (canvasVideo == null) isCanvasReady = false
    }


    val grayscaleColorFilter = androidx.compose.runtime.remember {
        androidx.compose.ui.graphics.ColorFilter.colorMatrix(
            androidx.compose.ui.graphics.ColorMatrix(
                floatArrayOf(
                    0.333f, 0.333f, 0.333f, 0f, 0f,
                    0.333f, 0.333f, 0.333f, 0f, 0f,
                    0.333f, 0.333f, 0.333f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            ).apply {
                val c = 1.2f // Enhanced contrast for e-ink
                val t = (1f - c) / 2f * 255f
                timesAssign(
                    androidx.compose.ui.graphics.ColorMatrix(
                        floatArrayOf(
                            c, 0f, 0f, 0f, t,
                            0f, c, 0f, 0f, t,
                            0f, 0f, c, 0f, t,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
        )
    }


    LaunchedEffect(playerConnection, isPlaying) {
        while (true) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it > 0L }
                ?: ((mediaMetadata?.duration ?: 0) * 1000L)
            sliderPosition = if (duration > 0L) position.toFloat() / duration else 0f
            delay(500)
        }
    }

    val isSpotifyThumbnailActive = canvasVideo != null && canvasVideo.source == CanvasSource.SPOTIFY && isCanvasReady

    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides androidx.compose.ui.graphics.Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Color.White)
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
                        val artworkUrl = mediaMetadata?.thumbnailUrl
                        val contentGap = 6.dp

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 6.dp),
                            verticalArrangement = Arrangement.Bottom,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = if (isSpotifyThumbnailActive) {
                                        Modifier
                                            .width(140.dp)
                                            .aspectRatio(9f / 16f)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                    } else {
                                        Modifier
                                            .size(140.dp)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                    }
                                ) {
                                    // Placeholder when artwork and canvas don't exist
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(androidx.compose.ui.graphics.Color.White)
                                            .border(
                                                width = 1.5.dp,
                                                color = androidx.compose.ui.graphics.Color.Black,
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.MusicNote,
                                            contentDescription = null,
                                            tint = androidx.compose.ui.graphics.Color.Black,
                                            modifier = Modifier.size(56.dp)
                                        )
                                    }

                                    if (!artworkUrl.isNullOrBlank()) {
                                        coil3.compose.AsyncImage(
                                            model = artworkUrl,
                                            contentDescription = null,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            colorFilter = grayscaleColorFilter,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    if (canvasVideo != null) {
                                        CanvasArtworkPlayer(
                                            source = canvasVideo.source,
                                            primaryUrl = canvasVideo.animated ?: canvasVideo.animatedVertical ?: canvasVideo.videoUrl,
                                            fallbackUrl = canvasVideo.videoUrl ?: canvasVideo.videoUrlVertical,
                                            isPlaying = isPlaying,
                                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .alpha(if (isCanvasReady) 1f else 0f),
                                            onReady = { isCanvasReady = it }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    IconButton(
                                        onClick = { playerConnection.seekToPrevious() },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.SkipPrevious,
                                            contentDescription = "Previous Song",
                                            tint = androidx.compose.ui.graphics.Color.Black,
                                            modifier = Modifier.size(32.dp),
                                        )
                                    }

                                    IconButton(
                                        onClick = { player.togglePlayPause() },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) OutlinedPause else Icons.Outlined.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = androidx.compose.ui.graphics.Color.Black,
                                            modifier = Modifier.size(32.dp),
                                        )
                                    }

                                    IconButton(
                                        onClick = { playerConnection.seekToNext() },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.SkipNext,
                                            contentDescription = "Next Song",
                                            tint = androidx.compose.ui.graphics.Color.Black,
                                            modifier = Modifier.size(32.dp),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(contentGap * 2))

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                            ) {
                                Text(
                                    text = mediaMetadata?.title.orEmpty().uppercase(),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily(
                                        androidx.compose.ui.text.font.Font(
                                            moe.rukamori.archivetune.R.font.clarity_city_black,
                                            FontWeight.Black
                                        )
                                    ),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 18.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = mediaMetadata?.artists?.joinToString(", ") { it.name }.orEmpty(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Spacer(modifier = Modifier.height(contentGap))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                PaperProgressBar(
                                    progress = sliderPosition.coerceIn(0f, 1f),
                                    modifier = Modifier.fillMaxWidth(),
                                    height = 16.dp,
                                    isIndeterminate = isBuffering,
                                    onProgressChange = { value ->
                                        sliderPosition = value
                                        if (duration > 0) {
                                            val newPosition = (value * duration).toLong().coerceIn(0L, duration)
                                            player.seekTo(newPosition)
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = makeTimeString(position),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = androidx.compose.ui.graphics.Color.Black,
                                    )
                                    Text(
                                        text = makeTimeString(duration),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = androidx.compose.ui.graphics.Color.Black,
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
                                    if (index == currentWindowIndex) {
                                        player.togglePlayPause()
                                    } else {
                                        player.seekToDefaultPosition(window.firstPeriodIndex)
                                        player.playWhenReady = true
                                    }
                                },
                                showDivider = index != queueWindows.lastIndex,
                            )
                        }
                    }
                }
            }
        }
        
        ApplicationBar(
            actions = buildList {
                add(
                    AppbarAction(
                        icon = Icons.Outlined.Shuffle,
                        label = if (shuffleEnabled) "Shuffle on" else "Shuffle off",
                        isActive = shuffleEnabled,
                        onClick = { player.shuffleModeEnabled = !shuffleEnabled }
                    )
                )
                add(
                    AppbarAction(
                        icon = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> Icons.Outlined.RepeatOne
                            else -> Icons.Outlined.Repeat
                        },
                        label = when (repeatMode) {
                            Player.REPEAT_MODE_ALL -> "Repeat all"
                            Player.REPEAT_MODE_ONE -> "Repeat one"
                            else -> "Repeat off"
                        },
                        isActive = repeatMode != Player.REPEAT_MODE_OFF,
                        onClick = { player.toggleRepeatMode() }
                    )
                )
                if (!isDownloaded) {
                    val isDownloading = download?.state == Download.STATE_QUEUED || download?.state == Download.STATE_DOWNLOADING
                    add(
                        AppbarAction(
                            icon = if (isDownloading) Icons.Outlined.Downloading else Icons.Outlined.Download,
                            label = if (isDownloading) "Downloading" else "Download",
                            onClick = {
                                if (isDownloading) {
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
                                        if (!isLibrarySong) {
                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                                val remoteResult = moe.rukamori.archivetune.innertube.YouTube.likeVideo(metadata.id, true)
                                                if (remoteResult.isSuccess) {
                                                    val now = java.time.LocalDateTime.now()
                                                    database.withTransaction {
                                                        val base = librarySong?.song ?: metadata.toSongEntity()
                                                        if (librarySong == null) {
                                                            insert(metadata, { it.toggleLibrary() })
                                                        } else {
                                                            update(base.copy(inLibrary = now))
                                                        }
                                                    }
                                                }
                                            }
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
                                }
                            }
                        )
                    )
                }
            },
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
                if (isDownloaded) {
                    add(
                        AppbarMenuItem(
                            label = "Remove download",
                            onClick = {
                                mediaMetadata?.let { metadata ->
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        metadata.id,
                                        false,
                                    )
                                }
                            }
                        )
                    )
                }
            },
            pagerState = pagerState,
            onBack = { navController.navigateUp() }
        )
    }
    }
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
                    contentDescription = "Now playing", tint = androidx.compose.material3.LocalContentColor.current,
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

