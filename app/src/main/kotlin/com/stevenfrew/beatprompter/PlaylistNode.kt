package com.stevenfrew.beatprompter

import com.stevenfrew.beatprompter.cache.SongFile

class PlaylistNode internal constructor(var mSongFile: SongFile,previousNode:PlaylistNode?) {
    internal var mNextNode: PlaylistNode? = null
    init {
        previousNode?.mNextNode=this
    }
}