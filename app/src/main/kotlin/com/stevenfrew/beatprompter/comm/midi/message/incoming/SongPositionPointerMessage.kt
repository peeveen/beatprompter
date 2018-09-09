package com.stevenfrew.beatprompter.comm.midi.message.incoming

class SongPositionPointerMessage internal constructor(bytes: ByteArray) : IncomingMessage() {
    val mMidiBeat= calculateMidiBeat(bytes)

    companion object {
        private fun calculateMidiBeat(bytes:ByteArray):Int
        {
            var firstHalf = bytes[1].toInt()
            firstHalf = firstHalf shl 7
            val secondHalf = bytes[0].toInt()
            return firstHalf or secondHalf
        }
    }
}