package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.midi.EventOffset
import com.stevenfrew.beatprompter.midi.OutgoingMessage

/**
 * A MIDIEvent tells the event processor to chuck some MIDI data out of the USB port.
 */
class MIDIEvent(time: Long, val mMessages: List<OutgoingMessage>, val mOffset: EventOffset? = null) : BaseEvent(time)