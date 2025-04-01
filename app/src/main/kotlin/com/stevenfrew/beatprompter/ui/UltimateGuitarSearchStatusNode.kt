package com.stevenfrew.beatprompter.ui

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.ContentParsingError
import com.stevenfrew.beatprompter.cache.parse.TextContentProvider
import com.stevenfrew.beatprompter.midi.SongTrigger
import com.stevenfrew.beatprompter.song.SongInfo
import com.stevenfrew.beatprompter.song.SongInfoProvider
import com.stevenfrew.beatprompter.util.Utils.sortableString
import com.stevenfrew.beatprompter.util.normalize
import java.util.Date

class UltimateGuitarSearchStatusNode(private val searchStatus: UltimateGuitarSearchStatus) :
	SongInfoProvider, SongInfo {
	override val songInfo: SongInfo = this
	override val title
		get() = BeatPrompter.appResources.getString(
			getSearchStatusText(searchStatus)
		)
	override val artist
		get() = BeatPrompter.appResources.getString(
			getSearchStatusSubtext(searchStatus),
			UltimateGuitarListAdapter.MINIMUM_SEARCH_TEXT_LENGTH
		)
	override val normalizedTitle get() = title.normalize()
	override val normalizedArtist get() = artist.normalize()
	override val sortableTitle get() = sortableString(title)
	override val sortableArtist get() = sortableString(artist)
	override val votes = 0
	override val id = searchStatus.toString()
	override val icon = null
	override val isBeatScrollable = false
	override val isSmoothScrollable = false
	override val rating = 0
	override val year: Int? = null
	override val lastModified: Date = Date()
	override val variations = listOf<String>()
	override val mixedModeVariations = listOf<String>()
	override val firstChord: String? = null
	override val lines: Int = 1
	override val capo: Int = 0
	override val bars: Int = 4
	override val bpm: Double = 120.0
	override val programChangeTrigger: SongTrigger = SongTrigger.DEAD_TRIGGER
	override val songSelectTrigger: SongTrigger = SongTrigger.DEAD_TRIGGER
	override val chords = listOf<String>()
	override val errors = listOf<ContentParsingError>()
	override val duration: Long = 0L
	override val totalPauseDuration: Long = 0L
	override val songContentProvider: TextContentProvider
		get() = object : TextContentProvider {
			override fun getContent(): String = ""
		}
	override val keySignature = null
	override val audioFiles = mapOf<String, List<String>>()
	override val defaultVariation: String = ""

	override fun matchesTrigger(trigger: SongTrigger): Boolean = false

	companion object {
		private fun getSearchStatusText(searchStatus: UltimateGuitarSearchStatus): Int =
			when (searchStatus) {
				UltimateGuitarSearchStatus.NoResults -> R.string.no_results
				UltimateGuitarSearchStatus.Searching -> R.string.searching
				UltimateGuitarSearchStatus.NotEnoughSearchText -> R.string.not_enough_search_text
			}

		private fun getSearchStatusSubtext(searchStatus: UltimateGuitarSearchStatus): Int =
			when (searchStatus) {
				UltimateGuitarSearchStatus.NoResults -> R.string.try_some_different_search_terms
				UltimateGuitarSearchStatus.Searching -> R.string.please_wait
				UltimateGuitarSearchStatus.NotEnoughSearchText -> R.string.search_characters_required
			}
	}
}
