package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.song.SongInfo

open class SetListFilter internal constructor(
	name: String,
	songs: List<Pair<SongInfo, String?>>
) : SongFilter(name, songs, false) {
	fun containsSong(sf: SongInfo): Boolean = songs.any { it.first == sf }
	override fun equals(other: Any?): Boolean = other is SetListFilter && name == other.name
	override fun hashCode(): Int = javaClass.hashCode()
}