package com.stevenfrew.beatprompter.song

import com.stevenfrew.beatprompter.cache.parse.ContentParsingError
import com.stevenfrew.beatprompter.cache.parse.TextContentProvider
import com.stevenfrew.beatprompter.midi.MidiTrigger
import com.stevenfrew.beatprompter.midi.SongTrigger
import java.util.Date

interface SongInfo {
	val id: String
	val title: String
	val artist: String
	val normalizedTitle: String
	val normalizedArtist: String
	val sortableTitle: String
	val sortableArtist: String
	val icon: String?
	val isBeatScrollable: Boolean
	val isSmoothScrollable: Boolean
	val rating: Int
	val year: Int?
	val votes: Int
	val keySignature: String?
	val audioFiles: Map<String, List<String>>
	val defaultVariation: String
	val lastModified: Date
	val variations: List<String>
	val mixedModeVariations: List<String>
	val firstChord: String?
	val lines: Int
	val capo: Int
	val bars: Int
	val bpm: Double
	val programChangeTrigger: MidiTrigger
	val songSelectTrigger: MidiTrigger
	val chords: List<String>
	val errors: List<ContentParsingError>
	val duration: Long
	val totalPauseDuration: Long
	val songContentProvider: TextContentProvider
	fun matchesTrigger(trigger: SongTrigger): Boolean
}