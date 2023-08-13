package com.stevenfrew.beatprompter.set

import com.stevenfrew.beatprompter.cache.SongFile

class PlaylistNode internal constructor(val mSongFile: SongFile) {
	internal var mNextNode: PlaylistNode? = null
}
