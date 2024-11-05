package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.cache.SongFile

class TemporarySetListFilter(
	setListFile: SetListFile,
	songs: List<SongFile>
) : SetListFileFilter(setListFile, songs) {
	fun addSong(sf: SongFile) = songs.add(sf to null)
	override fun equals(other: Any?): Boolean = other != null && other is TemporarySetListFilter
	override fun hashCode(): Int = javaClass.hashCode()

	fun clear() {
		mMissingSetListEntries.clear()
		songs.clear()
	}
}
