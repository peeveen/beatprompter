package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SongFile

class VariationFilter(variation: String, songs: List<SongFile>) :
	SongFilter(variation, songs.map { it to null }, true) {
	override fun equals(other: Any?): Boolean = other is VariationFilter && name == other.name
	override fun hashCode(): Int = javaClass.hashCode()
}