package com.stevenfrew.beatprompter.ui.filter

import com.stevenfrew.beatprompter.cache.SetListFile
import com.stevenfrew.beatprompter.set.SetListEntry
import com.stevenfrew.beatprompter.song.SongInfo

open class SetListFileFilter(
	var setListFile: SetListFile,
	setSongs: List<SongInfo>
) : SetListFilter(setListFile.setTitle, getSongList(setListFile.setListEntries, setSongs)) {
	var mMissingSetListEntries = getMissingSetListEntries(
		setListFile.setListEntries,
		songs.map { it.first }
	)
	var mWarned = mMissingSetListEntries.isEmpty()

	companion object {
		private fun getSongList(
			setListEntries: List<SetListEntry>,
			songFiles: List<SongInfo>
		): List<Pair<SongInfo, String?>> =
			getMatches(setListEntries, songFiles)
				.filter { it.second != null || it.third != null }
				.map {
					(it.second
						?: it.third)!! to it.first.variation.ifBlank { null }
				}

		private fun getMissingSetListEntries(
			setListEntries: List<SetListEntry>,
			songFiles: List<SongInfo>
		): MutableList<SetListEntry> =
			getMatches(setListEntries, songFiles)
				.filter { it.second == null && it.third == null }
				.map { it.first }
				.toMutableList()

		private fun getMatches(
			setListEntries: List<SetListEntry>,
			songFiles: List<SongInfo>
		): List<Triple<SetListEntry, SongInfo?, SongInfo?>> =
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

		private fun songHasVariation(song: SongInfo, variation: String): Boolean =
			variation.isBlank() || song.variations.contains(variation)
	}
}
