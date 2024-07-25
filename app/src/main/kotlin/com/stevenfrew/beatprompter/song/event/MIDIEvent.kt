package com.stevenfrew.beatprompter.song.event

import com.stevenfrew.beatprompter.comm.midi.message.OutgoingMessage
import com.stevenfrew.beatprompter.midi.EventOffset

/**
 * A MIDIEvent tells the event processor to chuck some MIDI data out of the USB port.
 */
class MIDIEvent(
	time: Long,
	val messages: List<OutgoingMessage>,
	val offset: EventOffset = EventOffset(0)
) : BaseEvent(time) {
	override fun offset(nanoseconds: Long): BaseEvent =
		MIDIEvent(eventTime + nanoseconds, messages, offset)
}