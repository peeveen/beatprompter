package com.stevenfrew.beatprompter.set

import com.stevenfrew.beatprompter.song.SongInfo
import com.stevenfrew.beatprompter.song.SongInfoProvider
import com.stevenfrew.beatprompter.ui.UltimateGuitarListItem

data class PlaylistNode(
	override val songInfo: SongInfo,
	val variation: String? = null,
	val nextSong: PlaylistNode? = null
) : SongInfoProvider {
	constructor(ugListItem: UltimateGuitarListItem) : this(ugListItem, null, null)
}
