package com.stevenfrew.beatprompter.cache.parse.tag.midi.alias

import com.stevenfrew.beatprompter.cache.parse.tag.OncePerLine
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type

@OncePerLine
@TagName("with_midi_start")
@TagType(Type.Directive)
/**
 * Tag that defines that a MIDI alias should be sent along with a MIDI Start command.
 */
class WithMidiStartTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int
) : WithMidiTag(name, lineNumber, position, WithMidi.Start)