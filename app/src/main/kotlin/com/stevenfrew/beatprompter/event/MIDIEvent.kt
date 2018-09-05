package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.midi.EventOffset
import com.stevenfrew.beatprompter.midi.OutgoingMessage

class MIDIEvent(time: Long, val mMessages: List<OutgoingMessage>, val mOffset: EventOffset? = null) : BaseEvent(time)