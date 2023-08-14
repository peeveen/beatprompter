package com.stevenfrew.beatprompter.song.load

import com.stevenfrew.beatprompter.Logger

/**
 * Cancellation event that can be SET when we want to cancel the loading of a song.
 * The name of the song in the constructor is purely for debug logging purposes.
 */
class SongLoadCancelEvent(private val mSongName: String) {
	var isCancelled = false
		private set

	fun set() {
		Logger.logLoader { "Cancelling the load of '$mSongName'." }
		isCancelled = true
	}
}
