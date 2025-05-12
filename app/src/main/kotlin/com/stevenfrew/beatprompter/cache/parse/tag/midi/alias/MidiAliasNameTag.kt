package com.stevenfrew.beatprompter.cache.parse.tag.midi.alias

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.util.splitAndTrim

@OncePerLine
@TagName("midi_alias")
@TagType(Type.Directive)
/**
 * Tag that defines a MIDI alias name.
 */
class MidiAliasNameTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : ValueTag(name, lineNumber, position, value) {
	val aliasName: String
	val commandName: String?

	init {
		val bits = value.splitAndTrim(",")
		aliasName = bits[0]
		commandName = if (bits.size > 1 && bits[1].isNotEmpty())
			bits[1]
		else
			null
	}
}