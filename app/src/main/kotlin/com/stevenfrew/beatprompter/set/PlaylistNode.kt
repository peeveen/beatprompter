package com.stevenfrew.beatprompter.set

import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.song.SongInfoProvider

data class PlaylistNode(
	val songFile: SongFile,
	val variation: String? = null,
	val nextSong: PlaylistNode? = null
) : SongInfoProvider {
	override val songInfo = songFile
}
