package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.chord.Chord
import com.stevenfrew.beatprompter.chord.IChord
import com.stevenfrew.beatprompter.chord.InvalidChordException
import com.stevenfrew.beatprompter.chord.UnknownChord

@TagName("chord_map", "chordmap")
@TagType(Type.Directive)
/**
 * Transposes the chord map by the given amount.
 */
class ChordMapTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : Tag(name, lineNumber, position) {
	val from: String
	val to: IChord

	init {
		val bits = value.split('=')
		if ((bits.size != 2) || (bits.any { it.isBlank() }))
			throw MalformedTagException(BeatPrompter.appResources.getString(R.string.chordMapTagMustContainTwoValues))
		from = bits[0]
		to = try {
			Chord.parse(bits[1])
		} catch (_: InvalidChordException) {
			UnknownChord(bits[1])
		}
	}
}
