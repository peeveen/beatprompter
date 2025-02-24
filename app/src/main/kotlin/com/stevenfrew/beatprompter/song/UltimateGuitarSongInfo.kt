package com.stevenfrew.beatprompter.song

import com.github.peeveen.ultimateguitar.SongFetcher
import com.github.peeveen.ultimateguitar.TabInfo
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.ContentParsingError
import com.stevenfrew.beatprompter.cache.parse.TextContentProvider
import com.stevenfrew.beatprompter.midi.SongTrigger
import com.stevenfrew.beatprompter.util.Utils.sortableString
import com.stevenfrew.beatprompter.util.normalize
import java.util.Date
import kotlin.math.roundToInt

class UltimateGuitarSongInfo(private val tabInfo: TabInfo) : SongInfoProvider, SongInfo {
	override val songInfo: SongInfo = this
	override val title = tabInfo.songName
	override val artist = tabInfo.artistName
	override val normalizedTitle = title.normalize()
	override val normalizedArtist = artist.normalize()
	override val sortableTitle = sortableString(title)
	override val sortableArtist = sortableString(artist)
	override val votes get() = tabInfo.votes
	override val id get() = tabInfo.tabUrl
	override val icon: String? get() = null
	override val isBeatScrollable = false
	override val isSmoothScrollable = false
	override val rating: Int get() = tabInfo.rating.roundToInt()
	override val year: Int? = null
	override val lastModified: Date = Date()
	override val variations =
		listOf(BeatPrompter.appResources.getString(R.string.defaultVariationName))
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
		get() = object : TextContentProvider {
			override fun getContent(): String =
				SongFetcher.fetch(tabInfo)?.toChordPro()?.fold("") { a, v -> a + "\n" + v } ?: ""
		}
	override val keySignature: String? = tabInfo.key.ifBlank { null }
	override val audioFiles = mapOf<String, List<String>>()
	override val defaultVariation: String = ""

	override fun matchesTrigger(trigger: SongTrigger): Boolean = false
}
