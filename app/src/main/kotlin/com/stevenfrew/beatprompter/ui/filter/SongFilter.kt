package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SongFile

open abstract class SongFilter internal constructor(
	name: String,
	songs: List<SongFile>,
	canSort: Boolean
) : Filter(name, canSort) {
	val mSongs = songs.toMutableList()
}