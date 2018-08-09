package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.cache.SongFile

class PlaylistNode internal constructor(@JvmField var mSongFile: SongFile) {
    @JvmField internal var mNextNode: PlaylistNode? = null
    internal var mPrevNode: PlaylistNode? = null
}