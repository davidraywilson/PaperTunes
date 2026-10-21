package moe.rukamori.archivetune.eink.components

import androidx.media3.common.MediaItem
import moe.rukamori.archivetune.db.entities.AlbumWithSongs
import moe.rukamori.archivetune.playback.PlayerConnection
import moe.rukamori.archivetune.playback.queues.ListQueue
import moe.rukamori.archivetune.playback.queues.LocalAlbumRadio
import moe.rukamori.archivetune.playback.queues.Queue

// CUSTOM: E-Ink shuffle playback.
// Unlike the original UI (which pre-shuffles the list and leaves the player's shuffle mode off),
// E-Ink "Shuffle" buttons play the list in its natural order with the player's shuffle mode ON.
// This keeps the Now Playing shuffle toggle in sync and lets the user turn shuffle off to
// return to the original order.

/** Plays [items] with shuffle mode enabled, starting from a random song. */
fun PlayerConnection.playShuffled(
    items: List<MediaItem>,
    title: String? = null,
) {
    if (items.isEmpty()) return
    playShuffledQueue(ListQueue(title = title, items = items, startIndex = items.indices.random()))
}

/**
 * Plays [albumWithSongs] as a [LocalAlbumRadio] (album, then related radio, same as the original UI)
 * with shuffle mode enabled, starting from a random track.
 */
fun PlayerConnection.playShuffled(albumWithSongs: AlbumWithSongs) {
    if (albumWithSongs.songs.isEmpty()) return
    playShuffledQueue(LocalAlbumRadio(albumWithSongs, startIndex = albumWithSongs.songs.indices.random()))
}

private fun PlayerConnection.playShuffledQueue(queue: Queue) {
    // playQueue() synchronously resets shuffle mode (unless Permanent Shuffle is on) and then loads
    // the items asynchronously on the main thread. Enabling shuffle right after means the service
    // applies its current-first shuffle order once the items are in place.
    playQueue(queue)
    player.shuffleModeEnabled = true
}
