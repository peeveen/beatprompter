package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R

class ChannelValue internal constructor(channel: Byte) : ByteValue(channel) {
	init {
		if (mValue !in 0..15)
			throw ValueException(BeatPrompter.appResources.getString(R.string.invalid_channel_value))
	}

	override fun toString(): String {
		return "#$mValue"
	}
}