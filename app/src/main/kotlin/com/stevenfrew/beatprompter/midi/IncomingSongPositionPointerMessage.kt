package com.stevenfrew.beatprompter.midi

class IncomingSongPositionPointerMessage internal constructor(bytes: ByteArray) : IncomingMessage(bytes) {
    val midiBeat: Int
        get() {
            var firstHalf = mMessageBytes[2].toInt()
            firstHalf = firstHalf shl 7
            val secondHalf = mMessageBytes[1].toInt()
            return firstHalf or secondHalf
        }
}