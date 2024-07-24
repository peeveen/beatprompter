package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.set.SetListEntry

open class SetListFileFilter(
	var setListFile: SetListFile,
	songs: List<SongFile>
) : SetListFilter(setListFile.setTitle, getSongList(setListFile.setListEntries, songs)) {
	var mMissingSetListEntries = getMissingSetListEntries(
		setListFile.setListEntries,
		this.songs
	)
	var mWarned = mMissingSetListEntries.isEmpty()

	companion object {
		private fun getSongList(
			setListEntries: List<SetListEntry>,
			songFiles: List<SongFile>
		): List<SongFile> = getMatches(setListEntries, songFiles).mapNotNull { it.second ?: it.third }

		private fun getMissingSetListEntries(
			setListEntries: List<SetListEntry>,
			songFiles: List<SongFile>
		): MutableList<SetListEntry> =
			getMatches(setListEntries, songFiles)
				.filter { it.second == null && it.third == null }
				.map { it.first }
				.toMutableList()

		private fun getMatches(
			setListEntries: List<SetListEntry>,
			songFiles: List<SongFile>
		): List<Triple<SetListEntry, SongFile?, SongFile?>> =
			setListEntries.map { entry ->
				val exactMatch =
					songFiles.firstOrNull { song: SongFile -> entry.matches(song) == SetListMatch.TitleAndArtistMatch }
				val inexactMatch =
					songFiles.firstOrNull { song: SongFile -> entry.matches(song) == SetListMatch.TitleMatch }
				Triple(entry, exactMatch, inexactMatch)
			}
	}
}
