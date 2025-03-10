package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SongFile

class TagFilter(tag: String, songs: List<SongFile>) :
	SongFilter(tag, songs.map { it to null }, true) {
	override fun equals(other: Any?): Boolean = other is TagFilter && name == other.name
	override fun hashCode(): Int = javaClass.hashCode()
}