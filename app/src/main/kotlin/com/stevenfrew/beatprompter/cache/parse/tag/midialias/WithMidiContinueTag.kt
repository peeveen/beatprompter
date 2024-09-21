package com.stevenfrew.beatprompter.cache.parse.tag.midialias

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerLine
@TagName("with_midi_continue")
@TagType(Type.Directive)
/**
 * Tag that defines that a MIDI alias should be sent along with a MIDI Continue command.
 */
class WithMidiContinueTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int
) : WithMidiTag(name, lineNumber, position, WithMidi.Continue)