package com.stevenfrew.beatprompter.cache.parse.tag.midi.alias

import com.stevenfrew.beatprompter.cache.parse.tag.Tag

/**
 * Tag that defines that a MIDI alias should be sent along with a MIDI Start, Stop, or Continue command.
 */
abstract class WithMidiTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	internal val with: WithMidi
) : Tag(name, lineNumber, position)

enum class WithMidi {
	SongLoad,
	Start,
	Stop,
	Continue
}
