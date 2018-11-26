package com.stevenfrew.beatprompter.comm.midi.message

import com.stevenfrew.beatprompter.comm.OutgoingMessage
import kotlin.experimental.and
import kotlin.experimental.or

open class Message constructor(bytes: ByteArray) : OutgoingMessage(bytes) {
    companion object {
        internal const val MIDI_SYSEX_START_BYTE = 0xf0.toByte()
        internal const val MIDI_SONG_POSITION_POINTER_BYTE = 0xf2.toByte()
        internal const val MIDI_SONG_SELECT_BYTE = 0xf3.toByte()
        internal const val MIDI_START_BYTE = 0xfa.toByte()
        internal const val MIDI_CONTINUE_BYTE = 0xfb.toByte()
        internal const val MIDI_STOP_BYTE = 0xfc.toByte()
        internal const val MIDI_SYSEX_END_BYTE = 0xf7.toByte()

        internal const val MIDI_CONTROL_CHANGE_BYTE = 0xb0.toByte()
        internal const val MIDI_PROGRAM_CHANGE_BYTE = 0xc0.toByte()

        internal const val MIDI_MSB_BANK_SELECT_CONTROLLER = 0.toByte()
        internal const val MIDI_LSB_BANK_SELECT_CONTROLLER = 32.toByte()

        fun mergeMessageByteWithChannel(message: Byte, channel: Byte): Byte {
            return (message and 0xf0.toByte()) or (channel and 0x0f)
        }

        fun getChannelFromBitmask(bitmask: Int): Byte {
            var n = 1
            var counter: Byte = 0
            do {
                if (n == bitmask)
                    return counter
                n = n shl 1
                ++counter
            } while (counter < 16)
            return 0
        }
    }

    private fun isSystemCommonMessage(message: Byte): Boolean {
        return mBytes.isNotEmpty() && mBytes[0] == message
    }

    private fun isChannelVoiceMessage(message: Byte): Boolean {
        return mBytes.isNotEmpty() && (mBytes[0] and 0xF0.toByte() == message)
    }

    fun isStart(): Boolean {
        return isSystemCommonMessage(MIDI_START_BYTE)
    }

    fun isStop(): Boolean {
        return isSystemCommonMessage(MIDI_STOP_BYTE)
    }

    fun isContinue(): Boolean {
        return isSystemCommonMessage(MIDI_CONTINUE_BYTE)
    }

    fun isSongPositionPointer(): Boolean {
        return isSystemCommonMessage(MIDI_SONG_POSITION_POINTER_BYTE)
    }

    internal fun isSongSelect(): Boolean {
        return isSystemCommonMessage(MIDI_SONG_SELECT_BYTE)
    }

    internal fun isProgramChange(): Boolean {
        return isChannelVoiceMessage(MIDI_PROGRAM_CHANGE_BYTE)
    }

    private fun isControlChange(): Boolean {
        return isChannelVoiceMessage(MIDI_CONTROL_CHANGE_BYTE)
    }

    internal fun getMIDIChannel(): Byte {
        return mBytes[0] and 0x0F
    }

    internal fun getBankSelectValue(): Int {
        return mBytes[2].toInt()
    }

    internal fun getSongSelectValue(): Int {
        return mBytes[2].toInt()
    }

    internal fun getProgramChangeValue(): Int {
        return mBytes[1].toInt()
    }

}