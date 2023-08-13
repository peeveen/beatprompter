package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.cache.SongFile

class TemporarySetListFilter(
	setListFile: SetListFile,
	songs: List<SongFile>
) : SetListFileFilter(setListFile, songs) {
	fun addSong(sf: SongFile) {
		mSongs.add(sf)
	}

	override fun equals(other: Any?): Boolean {
		return other != null && other is TemporarySetListFilter
	}

	fun clear() {
		mMissingSetListEntries.clear()
		mSongs.clear()
	}

	override fun hashCode(): Int {
		return javaClass.hashCode()
	}
}
