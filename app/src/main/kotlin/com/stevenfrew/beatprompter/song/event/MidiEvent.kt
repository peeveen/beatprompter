package com.stevenfrew.beatprompter.song.event

import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage
import com.stevenfrew.beatprompter.midi.EventOffset

/**
 * A MIDIEvent tells the event processor to chuck some MIDI data out of the USB port.
 */
class MidiEvent(
	time: Long,
	val messages: List<MidiMessage>,
	val offset: EventOffset = EventOffset(0)
) : BaseEvent(time) {
	override fun offset(nanoseconds: Long): BaseEvent =
		MidiEvent(eventTime + nanoseconds, messages, offset)
}