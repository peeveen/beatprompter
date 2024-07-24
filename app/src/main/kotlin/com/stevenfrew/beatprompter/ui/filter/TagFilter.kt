package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SongFile

class TagFilter(tag: String, songs: MutableList<SongFile>) : SongFilter(tag, songs, true) {

	override fun equals(other: Any?): Boolean = other is TagFilter && name == other.name

	override fun hashCode(): Int = javaClass.hashCode()
}