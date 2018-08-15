package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.midi.EventOffset
import com.stevenfrew.beatprompter.midi.OutgoingMessage

class MIDIEvent : BaseEvent {
    val mMessages: List<OutgoingMessage>
    val mOffset: EventOffset

    constructor(time: Long, messages: List<OutgoingMessage>) : this(time,messages, EventOffset.NoOffset)

    constructor(time: Long, message: OutgoingMessage, offset: EventOffset) : this(time,listOf(message),offset)

    constructor(time: Long, messages: List<OutgoingMessage>, offset: EventOffset) : super(time) {
        mMessages = messages
        mOffset = offset
    }
}