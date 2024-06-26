package com.stevenfrew.beatprompter.cache.parse.tag.midialias

import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@TagType(Type.Directive)
/**
 * Tag that contains MIDI alias instructions.
 */
class MIDIAliasInstructionTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	val mInstructions: String
) : Tag(name, lineNumber, position)