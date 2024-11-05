package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.set.SetListEntry

open class SetListFileFilter(
	var setListFile: SetListFile,
	setSongs: List<SongFile>
) : SetListFilter(setListFile.setTitle, getSongList(setListFile.setListEntries, setSongs)) {
	var mMissingSetListEntries = getMissingSetListEntries(
		setListFile.setListEntries,
		songs.map { it.first }
	)
	var mWarned = mMissingSetListEntries.isEmpty()

	companion object {
		private fun getSongList(
			setListEntries: List<SetListEntry>,
			songFiles: List<SongFile>
		): List<Pair<SongFile, String?>> =
			getMatches(setListEntries, songFiles)
				.filter { it.second != null || it.third != null }
				.map {
					(it.second
						?: it.third)!! to it.first.variation.ifBlank { null }
				}

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
					songFiles.firstOrNull { song ->
						songHasVariation(song, entry.variation) && entry.matches(
							song
						) == SetListMatch.TitleAndArtistMatch
					}
				val inexactMatch =
					songFiles.firstOrNull { song ->
						songHasVariation(song, entry.variation) && entry.matches(
							song
						) == SetListMatch.TitleMatch
					}
				Triple(entry, exactMatch, inexactMatch)
			}

		private fun songHasVariation(song: SongFile, variation: String): Boolean =
			variation.isBlank() || song.variations.contains(variation)
	}
}
