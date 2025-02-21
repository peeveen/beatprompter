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
import com.stevenfrew.ultimateguitar.SongFetcher
import com.stevenfrew.ultimateguitar.TabInfo
import java.util.Date

class UltimateGuitarListItem : SongInfoProvider, SongInfo {
	val searchStatus: UltimateGuitarSearchStatus
	val tabInfo: TabInfo?

	constructor(searchStatus: UltimateGuitarSearchStatus) {
		this.searchStatus = searchStatus
		this.tabInfo = null
	}

	constructor(tabInfo: TabInfo) {
		this.tabInfo = tabInfo
		this.searchStatus = UltimateGuitarSearchStatus.Complete
	}

	override val songInfo: SongInfo = this
	override val title
		get() = tabInfo?.songName ?: BeatPrompter.appResources.getString(
			getSearchStatusText(searchStatus)
		)
	override val artist
		get() = tabInfo?.artistName ?: BeatPrompter.appResources.getString(
			getSearchStatusSubtext(searchStatus),
			UltimateGuitarListAdapter.MINIMUM_SEARCH_TEXT_LENGTH
		)
	override val normalizedTitle get() = title.normalize()
	override val normalizedArtist get() = artist.normalize()
	override val sortableTitle get() = sortableString(title)
	override val sortableArtist get() = sortableString(artist)
	override val votes get() = tabInfo?.votes ?: 0
	override val id get() = tabInfo?.tabUrl ?: searchStatus.toString()
	override val icon: String? get() = null
	override val isBeatScrollable = false
	override val isSmoothScrollable = false
	override val rating: Int get() = Math.round(tabInfo?.rating ?: 0.0).toInt()
	override val year: Int? = null
	override val lastModified: Date = Date()
	override val variations = listOf<String>()
	override val mixedModeVariations = listOf<String>()
	override val firstChord: String? = null
	override val lines: Int = 1
	override val bars: Int = 4
	override val bpm: Double = 120.0
	override val programChangeTrigger: SongTrigger = SongTrigger.DEAD_TRIGGER
	override val songSelectTrigger: SongTrigger = SongTrigger.DEAD_TRIGGER
	override val chords = listOf<String>()
	override val errors = listOf<ContentParsingError>()
	override val duration: Long = 0L
	override val totalPauseDuration: Long = 0L
	override val songContentProvider: TextContentProvider
		get() = if (tabInfo == null) object : TextContentProvider {
			override fun getContent(): String = ""
		} else object : TextContentProvider {
			override fun getContent(): String =
				SongFetcher.fetch(tabInfo)?.toChordPro()?.fold("") { a, v -> a + "\n" + v } ?: ""
		}
	override val keySignature: String? get() = tabInfo?.key
	override val audioFiles = mapOf<String, List<String>>()
	override val defaultVariation: String = ""

	override fun matchesTrigger(trigger: SongTrigger): Boolean = false

	companion object {
		private fun getSearchStatusText(searchStatus: UltimateGuitarSearchStatus): Int =
			when (searchStatus) {
				UltimateGuitarSearchStatus.NoResults -> R.string.no_results
				UltimateGuitarSearchStatus.Searching -> R.string.searching
				UltimateGuitarSearchStatus.NotEnoughSearchText -> R.string.not_enough_search_text
				else -> R.string.empty
			}

		private fun getSearchStatusSubtext(searchStatus: UltimateGuitarSearchStatus): Int =
			when (searchStatus) {
				UltimateGuitarSearchStatus.NoResults -> R.string.try_some_different_search_terms
				UltimateGuitarSearchStatus.Searching -> R.string.please_wait
				UltimateGuitarSearchStatus.NotEnoughSearchText -> R.string.search_characters_required
				else -> R.string.empty
			}
	}
}
