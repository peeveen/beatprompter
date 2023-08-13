package com.stevenfrew.beatprompter.cache.parse.tag.midialias

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerLine
@TagName("midi_alias")
@TagType(Type.Directive)
/**
 * Tag that defines a MIDI alias name.
 */
class MIDIAliasNameTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	val mAliasName: String
) : Tag(name, lineNumber, position)