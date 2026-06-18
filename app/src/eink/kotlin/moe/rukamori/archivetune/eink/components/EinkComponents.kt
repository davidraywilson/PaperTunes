/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.eink.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.offline.Download
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.painterResource
import com.mudita.mmd.components.checkbox.CheckboxMMD
import com.mudita.mmd.components.menus.DropdownMenuMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.mudita.mmd.components.text.TextMMD
import moe.rukamori.archivetune.LocalDownloadUtil
import moe.rukamori.archivetune.db.entities.Album
import moe.rukamori.archivetune.db.entities.Artist
import moe.rukamori.archivetune.db.entities.Playlist
import moe.rukamori.archivetune.db.entities.Song
import moe.rukamori.archivetune.utils.makeTimeString

/**
 * Dashed divider used between list rows. Ported from CalmTunes' DashedDivider so the
 * e-ink list aesthetic matches the reference design exactly.
 */
@Composable
fun DashedDivider(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    thickness: Dp = 1.dp,
    dashWidth: Dp = 2.dp,
    dashGap: Dp = 2.dp,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness),
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = thickness.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashWidth.toPx(), dashGap.toPx()),
                0f,
            ),
        )
    }
}

/** Builds the "Artist • Album • Duration" subtitle used by song rows. */
fun songSubtitle(song: Song): String {
    val artist = song.artists.joinToString(", ") { it.name }
    val album = song.album?.title?.takeIf { it.isNotBlank() }
    val duration = song.song.duration.takeIf { it > 0 }?.let { makeTimeString(it * 1000L) }
    return buildString {
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
}

/**
 * Song row matching CalmTunes' SongItem: bold 20sp title, 16sp subtitle, a "now playing"
 * headphones icon when active, optional leading track number, and a trailing dashed divider.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EinkSongRow(
    song: Song,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trackNumber: Int? = null,
    onLongClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    dropdownContent: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val downloadUtil = LocalDownloadUtil.current
    val downloadsMap by downloadUtil.downloads.collectAsState()
    val downloadState = downloadsMap[song.id]?.state

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        if (dropdownContent != null) {
                            expanded = true
                        }
                        onLongClick?.invoke()
                    },
                )
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
                } else if (trackNumber != null) {
                    TextMMD(
                        text = trackNumber.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.Center,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    TextMMD(
                        text = song.song.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (song.song.isLocal) {
                            Icon(
                                painter = painterResource(id = moe.rukamori.archivetune.R.drawable.snippet_folder),
                                contentDescription = "Local file",
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            when (downloadState) {
                            Download.STATE_COMPLETED -> {
                                Icon(
                                    painter = painterResource(id = moe.rukamori.archivetune.R.drawable.offline),
                                    contentDescription = "Downloaded",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                CircularProgressIndicatorMMD(
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                        }
                        TextMMD(
                            text = songSubtitle(song),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            if (showDivider) DashedDivider(thickness = 1.dp)
        }

        if (dropdownContent != null) {
            DropdownMenuMMD(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(x = 16.dp, y = 0.dp)
            ) {
                dropdownContent { expanded = false }
            }
        }
    }
}

/**
 * Two-line row used by Albums, Artists and Playlists: bold 20sp title, optional 16sp
 * subtitle, trailing dashed divider. Mirrors CalmTunes' PlaylistItem layout.
 */
@Composable
fun EinkTwoLineRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(bottom = 8.dp),
    ) {
        TextMMD(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            TextMMD(
                text = subtitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (showDivider) DashedDivider(thickness = 1.dp)
    }
}

/** Selectable two-line row (leading MMD checkbox) used by multi-select screens. */
@Composable
fun EinkSelectableRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckboxMMD(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.padding(0.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                TextMMD(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextMMD(
                        text = subtitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (showDivider) DashedDivider(thickness = 1.dp)
    }
}

/** Centered empty-state message used by library screens with no content. */
@Composable
fun EinkEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextMMD(text = title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            TextMMD(
                text = body,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Subtitle helpers for the simple two-line rows. */
fun albumSubtitle(album: Album): String = album.artists.joinToString(", ") { it.name }

fun artistSubtitle(artist: Artist): String {
    val songs = artist.songCount
    return if (songs == 1) "1 song" else "$songs songs"
}

fun playlistSubtitle(playlist: Playlist): String {
    val count = playlist.songCount
    return if (count == 1) "1 song" else "$count songs"
}
