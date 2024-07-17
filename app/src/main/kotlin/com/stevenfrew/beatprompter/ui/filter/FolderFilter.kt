package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SongFile

class FolderFilter(
	folderName: String,
	songs: List<SongFile>
) : SongFilter(folderName, songs, true) {
	override fun equals(other: Any?): Boolean = other is FolderFilter && mName == other.mName

	override fun hashCode(): Int = javaClass.hashCode()
}