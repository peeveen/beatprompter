package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.cache.SongFile

class PlaylistNode internal constructor(var mSongFile: SongFile) {
    internal var mNextNode: PlaylistNode? = null
    internal var mPrevNode: PlaylistNode? = null
}