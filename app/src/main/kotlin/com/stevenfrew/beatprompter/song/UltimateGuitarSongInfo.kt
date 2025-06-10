package com.stevenfrew.beatprompter.song

import com.github.peeveen.ultimateguitar.Song
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
	override val songInfo = this
	override val title = tabInfo.songName
	override val artist = tabInfo.artistName
	override val normalizedTitle = title.normalize()
	override val normalizedArtist = artist.normalize()
	override val sortableTitle = sortableString(title)
	override val sortableArtist = sortableString(artist)
	override val votes get() = tabInfo.votes
	override val id get() = tabInfo.tabUrl
	override val icon get() = null
	override val isBeatScrollable = false
	override val isSmoothScrollable = false
	override val rating get() = tabInfo.rating.roundToInt()
	override val year = null
	override val lastModified = Date()
	override val variations =
		listOf(BeatPrompter.appResources.getString(R.string.defaultVariationName))
	override val mixedModeVariations = listOf<String>()
	override val firstChord = null
	override val lines = 1
	override val bars = 4
	override val capo get() = getSong()?.tabView?.meta?.capo ?: 0
	override val bpm: Double = 120.0
	override val programChangeTrigger = SongTrigger.DEAD_TRIGGER
	override val songSelectTrigger = SongTrigger.DEAD_TRIGGER
	override val chords = listOf<String>()
	override val tags = setOf<String>()
	override val errors = listOf<ContentParsingError>()
	override val duration = 0L
	override val totalPauseDuration = 0L
	override val songContentProvider
		get() = object : TextContentProvider {
			override fun getContent(): String {
				val songContent = getSong()?.toChordPro()?.fold("") { a, v -> a + "\n" + v } ?: ""
				return songContent
			}
		}
	override val keySignature = tabInfo.key.ifBlank { null }
	override val audioFiles = mapOf<String, List<String>>()
	override val defaultVariation = ""
	override fun matchesTrigger(trigger: SongTrigger): Boolean = false

	private var fetchedSong: Song? = null
	private fun getSong(): Song? {
		if (fetchedSong != null)
			return fetchedSong
		fetchedSong = SongFetcher.fetch(tabInfo)
		return fetchedSong
	}
}
