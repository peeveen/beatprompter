package com.stevenfrew.beatprompter.song.event

import com.stevenfrew.beatprompter.comm.midi.message.OutgoingMessage
import com.stevenfrew.beatprompter.midi.EventOffset

/**
 * A MIDIEvent tells the event processor to chuck some MIDI data out of the USB port.
 */
class MIDIEvent(time: Long,
                val mMessages: List<OutgoingMessage>,
                val mOffset: EventOffset? = null)
    : BaseEvent(time)