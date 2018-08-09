package com.stevenfrew.beatprompter.event

import com.stevenfrew.beatprompter.midi.EventOffset
import com.stevenfrew.beatprompter.midi.OutgoingMessage
import java.util.ArrayList

class MIDIEvent : BaseEvent {
    @JvmField var mMessages: MutableList<OutgoingMessage>
    @JvmField var mOffset: EventOffset?=null

    constructor(time: Long, messages: MutableList<OutgoingMessage>) : super(time) {
        mMessages = messages
    }

    private constructor(time: Long, message: OutgoingMessage) : super(time) {
        mMessages = ArrayList()
        mMessages.add(message)
    }

    constructor(time: Long, messages: MutableList<OutgoingMessage>, offset: EventOffset) : this(time, messages) {
        mOffset = offset
    }

    constructor(time: Long, message: OutgoingMessage, offset: EventOffset) : this(time, message) {
        mOffset = offset
    }
}