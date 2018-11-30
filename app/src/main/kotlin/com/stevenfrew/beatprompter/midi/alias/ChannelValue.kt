package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R

class ChannelValue internal constructor(channel: Byte) : ByteValue(channel) {
    init {
        if (mValue !in 0..15)
            throw ValueException(BeatPrompterApplication.getResourceString(R.string.invalid_channel_value))
    }
    override fun toString(): String {
        return "#$mValue"
    }
}