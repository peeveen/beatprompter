package com.stevenfrew.beatprompter.midi

class ChannelValue internal constructor(channel: Byte) : ByteValue(channel) {
    override fun toString(): String {
        return "#$mValue"
    }
}