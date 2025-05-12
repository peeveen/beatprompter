package com.stevenfrew.beatprompter.cache.parse.tag.midi.alias

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerLine
@TagName("midi_init")
@TagType(Type.Directive)
/**
 * Tag that defines that a MIDI alias should be sent when song loads.
 */
class MidiInitTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : Tag(name, lineNumber, position) {
	val order: Int = if (value.isBlank()) 0 else TagParsingUtility.parseIntegerValue(
		value,
		Int.MIN_VALUE,
		Int.MAX_VALUE
	)
}