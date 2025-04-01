package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.song.SongInfo

class TemporarySetListFilter(
	setListFile: SetListFile,
	songs: List<SongInfo>
) : SetListFileFilter(setListFile, songs) {
	fun addSong(sf: SongInfo) = songs.add(sf to null)
	override fun equals(other: Any?): Boolean = other != null && other is TemporarySetListFilter
	override fun hashCode(): Int = javaClass.hashCode()

	fun clear() {
		mMissingSetListEntries.clear()
		songs.clear()
	}
}
