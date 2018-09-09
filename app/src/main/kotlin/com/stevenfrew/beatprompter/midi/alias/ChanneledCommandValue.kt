package com.stevenfrew.beatprompter.midi.alias

import kotlin.experimental.and
import kotlin.experimental.or

class ChanneledCommandValue internal constructor(value: Byte) : ByteValue(value) {

    override fun resolve(arguments: ByteArray, channel: Byte): Byte {
        return ((mValue and 0xF0.toByte()) or (channel and 0x0F))
    }

    override fun matches(otherValue: Value?): Boolean {
        return if (otherValue is ChanneledCommandValue) otherValue.mValue == mValue else otherValue is WildcardValue
    }

    override fun toString(): String {
        val strHex = Integer.toHexString(mValue.toInt())
        return "0x" + strHex.takeLast(2).take(1) + "_"
    }
}