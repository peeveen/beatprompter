package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.midi.EventOffset
import com.stevenfrew.beatprompter.midi.OutgoingMessage

class MIDIEvent : BaseEvent {
    @JvmField var mMessages: List<OutgoingMessage>
    @JvmField var mOffset: EventOffset?=null

    constructor(time: Long, messages: List<OutgoingMessage>) : super(time) {
        mMessages = messages
    }

    private constructor(time: Long, message: OutgoingMessage) : super(time) {
        mMessages = listOf(message)
    }

    constructor(time: Long, messages: List<OutgoingMessage>, offset: EventOffset) : this(time, messages) {
        mOffset = offset
    }

    constructor(time: Long, message: OutgoingMessage, offset: EventOffset) : this(time, message) {
        mOffset = offset
    }
}