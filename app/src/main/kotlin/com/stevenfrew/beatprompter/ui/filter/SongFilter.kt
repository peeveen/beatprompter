package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.song.SongInfo

abstract class SongFilter internal constructor(
	name: String,
	songs: List<Pair<SongInfo, String?>>,
	canSort: Boolean
) : Filter(name, canSort) {
	val songs = songs.toMutableList()
}