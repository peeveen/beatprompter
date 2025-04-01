package com.stevenfrew.beatprompter.set

import com.stevenfrew.beatprompter.song.SongInfo
import com.stevenfrew.beatprompter.ui.filter.SetListMatch
import com.stevenfrew.beatprompter.util.splitAndTrim

/**
 * Represents one entry from a set list file.
 */
class SetListEntry private constructor(
	private val normalizedTitle: String,
	private val normalizedArtist: String,
	val variation: String
) {
	constructor(songInfo: SongInfo)
		: this(
		songInfo.normalizedTitle,
		songInfo.normalizedArtist,
		songInfo.defaultVariation
	)

	constructor(setListFileLine: String)
		: this(getInfoFromSetListLine(setListFileLine))

	// IDE says this is unused ... it is lying!
	@Suppress("unused")
	private constructor(titleAndArtist: Triple<String, String, String>)
		: this(titleAndArtist.first, titleAndArtist.second, titleAndArtist.third)

	fun matches(songFile: SongInfo): SetListMatch =
		if (songFile.normalizedTitle.equals(normalizedTitle, true)) {
			if (songFile.normalizedArtist.equals(normalizedArtist, true))
				SetListMatch.TitleAndArtistMatch
			SetListMatch.TitleMatch
		} else
			SetListMatch.NoMatch

	fun toDisplayString(): String =
		if (normalizedArtist.isBlank()) normalizedTitle
		else "$normalizedArtist - $normalizedTitle"

	override fun toString(): String = normalizedTitle + SET_LIST_ARTIST_DELIMITER + normalizedArtist

	companion object {
		private const val SET_LIST_ARTIST_DELIMITER = "==="
		private const val SET_LIST_VARIATION_DELIMITER = "###"

		private fun splitOnDelimiter(setListFileLine: String, delimiter: String): Pair<String, String> =
			setListFileLine.splitAndTrim(delimiter).let {
				if (it.size > 1)
					it[0].trim() to it[1].trim()
				else
					it[0].trim() to ""
			}

		private fun getInfoFromSetListLine(setListFileLine: String): Triple<String, String, String> =
			splitOnDelimiter(setListFileLine, SET_LIST_ARTIST_DELIMITER).let {
				if (it.second.isNotBlank()) {
					// Artist was specified.
					val secondSplit = splitOnDelimiter(it.second, SET_LIST_VARIATION_DELIMITER)
					Triple(it.first, secondSplit.first, secondSplit.second)
				} else {
					// No artist specified, but variation still might be
					val secondSplit = splitOnDelimiter(it.first, SET_LIST_VARIATION_DELIMITER)
					Triple(secondSplit.first, it.second, secondSplit.second)
				}
			}
	}
}
