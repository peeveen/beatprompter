package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import kotlin.experimental.and
import kotlin.experimental.or

class ChanneledCommandValue internal constructor(value: Byte) : ByteValue(value) {
	init {
		if (value and 0x0F != ZERO_BYTE)
			throw ValueException(BeatPrompter.appResources.getString(R.string.merge_with_channel_non_zero_lower_nibble))
	}

	override fun resolve(arguments: ByteArray, channel: Byte): Byte =
		((value and 0xF0.toByte()) or (channel and 0x0F))

	override fun matches(otherValue: Value?): Boolean =
		if (otherValue is ChanneledCommandValue) otherValue.value == value else otherValue is WildcardValue

	override fun toString(): String =
		"0x${Integer.toHexString(value.toInt()).takeLast(2).take(1)}_"
}