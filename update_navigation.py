import re
import os

files = [
    "app/src/eink/kotlin/moe/rukamori/papertunes/eink/screens/EinkArtistAlbumScreens.kt",
    "app/src/eink/kotlin/moe/rukamori/papertunes/eink/screens/EinkPlaylistScreens.kt",
    "app/src/eink/kotlin/moe/rukamori/papertunes/eink/screens/EinkSearchScreen.kt",
    "app/src/eink/kotlin/moe/rukamori/papertunes/eink/screens/EinkSongsScreen.kt",
    "app/src/eink/kotlin/moe/rukamori/papertunes/eink/screens/EinkYouTubeArtistScreens.kt"
]

def update_file(path):
    with open(path, 'r') as f:
        content = f.read()

    # We want to insert `navController.navigate(moe.rukamori.papertunes.eink.EinkScreen.NowPlaying.route)`
    # right after `playerConnection.playQueue(...)` closing brace of the `onClick = { ... }` or just after `playQueue(...)`
    # However, playQueue(...) often takes a ListQueue(...) argument over multiple lines.
    # It is safer to find the matching `onClick = {` or `onPlay = {` and insert it before the closing `}`.
    
    # Actually, the simplest string replace:
    # 1. find `playerConnection.player.togglePlayPause()`
    # 2. find `playerConnection.playQueue(...)` up to `)`
    # But since they are wrapped in an if-else:
    # if (song.id == mediaMetadata?.id) { playerConnection.player.togglePlayPause() } else { playerConnection.playQueue(...) }
    
    # We can just replace:
    # `playerConnection.playQueue(\n                                    ListQueue(\n...\n                                    ),\n                                )`
    pass

