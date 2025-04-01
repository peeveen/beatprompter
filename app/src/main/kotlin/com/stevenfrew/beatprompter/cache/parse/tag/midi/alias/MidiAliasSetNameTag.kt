package com.stevenfrew.beatprompter.cache.parse.tag.midi.alias

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.util.splitAndTrim

@OncePerFile
@TagName("midi_aliases")
@TagType(Type.Directive)
/**
 * Tag that defines a MIDI alias set name.
 */
class MidiAliasSetNameTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : ValueTag(name, lineNumber, position, value) {
	val aliasSetName: String
	val useByDefault: Boolean

	init {
		val bits = value.splitAndTrim(",")
		aliasSetName = bits[0]
		useByDefault = if (bits.size > 1)
			bits[1].toBoolean()
		else
			true
	}
}