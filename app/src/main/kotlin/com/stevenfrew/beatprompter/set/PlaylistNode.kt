package com.stevenfrew.beatprompter.set

import com.stevenfrew.beatprompter.cache.SongFile

class PlaylistNode internal constructor(var mSongFile: SongFile) {
    internal var mNextNode: PlaylistNode? = null
}
