package com.stevenfrew.beatprompter.set

import com.stevenfrew.beatprompter.song.SongInfo
import com.stevenfrew.beatprompter.song.SongInfoProvider
import com.stevenfrew.beatprompter.song.UltimateGuitarSongInfo
import com.stevenfrew.beatprompter.ui.UltimateGuitarSearchStatusNode

data class PlaylistNode(
	override val songInfo: SongInfo,
	val variation: String? = null,
	val nextSong: PlaylistNode? = null
) : SongInfoProvider {
	constructor(ugSearchNode: UltimateGuitarSearchStatusNode) : this(ugSearchNode, null, null)
	constructor(ugSongInfo: UltimateGuitarSongInfo) : this(ugSongInfo, null, null)
}
