package com.stevenfrew.beatprompter.set

import com.stevenfrew.beatprompter.cache.SongFile
import com.stevenfrew.beatprompter.ui.filter.SetListMatch
import com.stevenfrew.beatprompter.util.splitAndTrim

/**
 * Represents one entry from a set list file.
 */
class SetListEntry private constructor(
	private val normalizedTitle: String,
	private val normalizedArtist: String
) {
	private constructor(titleAndArtist: Pair<String, String>)
		: this(titleAndArtist.first, titleAndArtist.second)

	constructor(songFile: SongFile)
		: this(songFile.normalizedTitle, songFile.normalizedArtist)

	constructor(setListFileLine: String)
		: this(getTitleAndArtistFromSetListLine(setListFileLine))

	fun matches(songFile: SongFile): SetListMatch =
		if (songFile.normalizedTitle.equals(normalizedTitle, true)) {
			if (songFile.normalizedArtist.equals(normalizedArtist, true))
				SetListMatch.TitleAndArtistMatch
			SetListMatch.TitleMatch
		} else
			SetListMatch.NoMatch

	fun toDisplayString(): String =
		if (normalizedArtist.isBlank()) normalizedTitle
		else "$normalizedArtist - $normalizedTitle"

	override fun toString(): String = normalizedTitle + SET_LIST_ENTRY_DELIMITER + normalizedArtist

	companion object {
		private const val SET_LIST_ENTRY_DELIMITER = "==="

		private fun getTitleAndArtistFromSetListLine(setListFileLine: String): Pair<String, String> =
			setListFileLine.splitAndTrim(SET_LIST_ENTRY_DELIMITER).let {
				if (it.size > 1)
					it[0] to it[1]
				else
					it[0] to ""
			}
	}
}
