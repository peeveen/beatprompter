package com.stevenfrew.beatprompter.cache.parse.tag.midi.alias

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerLine
@TagType(Type.Directive)
@TagName("midi_channel")
/**
 * Tag that defines a default channel for the next MIDI aliases.
 */
class MidiAliasChannelTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : Tag(name, lineNumber, position) {
	val channel = TagParsingUtility.parseIntegerValue(value, 1, 16) - 1
}