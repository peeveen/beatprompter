package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SongFile

class FolderFilter(
	folderName: String,
	songs: List<SongFile>
) : SongFilter(folderName, songs, true) {
	override fun equals(other: Any?): Boolean = other is FolderFilter && name == other.name
	override fun hashCode(): Int = javaClass.hashCode()
}