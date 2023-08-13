package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.SongFile

class AllSongsFilter(songs: List<SongFile>) : SongFilter(
	BeatPrompter.getResourceString(R.string.no_tag_selected),
	songs, true
) {
	override fun equals(other: Any?): Boolean {
		return other == null || other is AllSongsFilter
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}