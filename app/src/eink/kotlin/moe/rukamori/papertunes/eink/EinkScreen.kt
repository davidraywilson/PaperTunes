/*
 * PaperTunes (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.papertunes.eink

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination

/**
 * Navigation destinations for the e-ink UI. Bottom-nav routes mirror CalmTunes
 * (Playlists, Artists, Songs, Albums, More); the remainder are detail/sub-screens.
 */
sealed class EinkScreen(val route: String, val label: String, val icon: ImageVector) {
    data object Playlists : EinkScreen("playlists", "Playlists", Icons.Outlined.LibraryMusic)
    data object Artists : EinkScreen("artists", "Artists", Icons.Outlined.PersonOutline)
    data object Songs : EinkScreen("songs", "Songs", Icons.AutoMirrored.Outlined.QueueMusic)
    data object Albums : EinkScreen("albums", "Albums", Icons.Outlined.Album)
    data object More : EinkScreen("more", "More", Icons.Outlined.MoreHoriz)

    // Non-bottom-nav destinations.
    data object PlaylistDetails : EinkScreen("playlistDetails", "Playlist", Icons.Outlined.LibraryMusic)
    data object PlaylistAddSongs : EinkScreen("playlistAddSongs", "Add Songs", Icons.Outlined.LibraryMusic)
    data object PlaylistEdit : EinkScreen("playlistEdit", "Edit Playlist", Icons.Outlined.LibraryMusic)
    data object AlbumDetails : EinkScreen("albumDetails", "Album", Icons.Outlined.Album)
    data object ArtistDetails : EinkScreen("artistDetails", "Artist", Icons.Outlined.PersonOutline)
    data object YouTubeArtistDetails : EinkScreen("youtubeArtistDetails", "Artist", Icons.Outlined.PersonOutline)
    data object Search : EinkScreen("search", "Search", Icons.AutoMirrored.Outlined.QueueMusic)
    data object Settings : EinkScreen("settings", "Settings", Icons.Outlined.MoreHoriz)
    data object NowPlaying : EinkScreen("nowPlaying", "Now Playing", Icons.AutoMirrored.Outlined.QueueMusic)
    data object Downloads : EinkScreen("downloads", "Downloads", Icons.Outlined.LibraryMusic)
}

/** Bottom navigation items, in CalmTunes order. */
val einkNavItems = listOf(
    EinkScreen.Playlists,
    EinkScreen.Artists,
    EinkScreen.Songs,
    EinkScreen.Albums,
    EinkScreen.More,
)

/** Resolves the top-app-bar title for the given destination. */
fun einkAppBarTitle(destination: NavDestination?): String =
    when (destination?.route) {
        EinkScreen.Playlists.route -> "Playlists"
        EinkScreen.Songs.route -> "Songs"
        EinkScreen.Albums.route -> "Albums"
        EinkScreen.AlbumDetails.route -> "Album"
        EinkScreen.Artists.route -> "Artists"
        EinkScreen.ArtistDetails.route -> "Artist"
        EinkScreen.YouTubeArtistDetails.route -> "Artist"
        EinkScreen.Search.route -> "Search"
        EinkScreen.More.route -> "More"
        EinkScreen.Settings.route -> "Settings"
        EinkScreen.PlaylistEdit.route -> "Edit Playlist"
        EinkScreen.PlaylistAddSongs.route -> "Add Songs"
        EinkScreen.PlaylistDetails.route -> "Playlist"
        EinkScreen.NowPlaying.route -> "Now Playing"
        EinkScreen.Downloads.route -> "Downloads"
        else -> ""
    }
