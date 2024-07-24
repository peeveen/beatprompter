package com.stevenfrew.beatprompter.cache.parse.tag.midialias

import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@TagType(Type.Directive)
/**
 * Tag that contains MIDI alias instructions.
 */
class MidiAliasInstructionTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	val instructions: String
) : Tag(name, lineNumber, position)