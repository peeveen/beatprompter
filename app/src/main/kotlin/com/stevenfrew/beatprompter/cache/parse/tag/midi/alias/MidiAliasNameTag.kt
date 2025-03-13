package com.stevenfrew.beatprompter.cache.parse.tag.midi.alias

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
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
) : Tag(name, lineNumber, position) {
	val aliasName: String
	val isCommand: Boolean

	init {
		val bits = value.splitAndTrim(",")
		aliasName = bits[0].ifBlank { throw MalformedTagException(R.string.tag_has_blank_value, name) }
		isCommand = if (bits.size > 1)
			bits[1].toBoolean()
		else
			false
	}
}