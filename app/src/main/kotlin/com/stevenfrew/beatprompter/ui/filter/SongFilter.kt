package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SongFile

abstract class SongFilter internal constructor(
	name: String,
	songs: List<Pair<SongFile, String?>>,
	canSort: Boolean
) : Filter(name, canSort) {
	val songs = songs.toMutableList()
}