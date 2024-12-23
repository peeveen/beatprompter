package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SongFile

open class SetListFilter internal constructor(
	name: String,
	songs: List<Pair<SongFile, String?>>
) : SongFilter(name, songs, false) {
	fun containsSong(sf: SongFile): Boolean = songs.any { it.first == sf }
	override fun equals(other: Any?): Boolean = other is SetListFilter && name == other.name
	override fun hashCode(): Int = javaClass.hashCode()
}