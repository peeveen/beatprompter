package com.stevenfrew.beatprompter.comm.midi.message

import com.stevenfrew.beatprompter.comm.Message
import kotlin.experimental.and
import kotlin.experimental.or

open class MidiMessage(bytes: ByteArray) : Message(bytes) {
	constructor(byte: Byte) : this(byteArrayOf(byte))
	constructor(byte1: Byte, byte2: Byte) : this(byteArrayOf(byte1, byte2))
	constructor(byte1: Byte, byte2: Byte, byte3: Byte) : this(byteArrayOf(byte1, byte2, byte3))

	companion object {
		internal const val ZERO_BYTE: Byte = 0

		internal const val MIDI_SYSEX_START_BYTE = 0xf0.toByte()
		internal const val MIDI_SONG_POSITION_POINTER_BYTE = 0xf2.toByte()
		internal const val MIDI_SONG_SELECT_BYTE = 0xf3.toByte()
		internal const val MIDI_CLOCK_BYTE = 0xf8.toByte()
		internal const val MIDI_START_BYTE = 0xfa.toByte()
		internal const val MIDI_CONTINUE_BYTE = 0xfb.toByte()
		internal const val MIDI_STOP_BYTE = 0xfc.toByte()
		internal const val MIDI_SYSEX_END_BYTE = 0xf7.toByte()

		internal const val MIDI_CONTROL_CHANGE_BYTE = 0xb0.toByte()
		internal const val MIDI_PROGRAM_CHANGE_BYTE = 0xc0.toByte()

		internal const val MIDI_MSB_BANK_SELECT_CONTROLLER: Byte = 0
		internal const val MIDI_LSB_BANK_SELECT_CONTROLLER = 32.toByte()

		internal fun mergeMessageByteWithChannel(message: Byte, channel: Byte): Byte =
			(message and 0xf0.toByte()) or (channel and 0x0f)

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

	private fun isSystemCommonMessage(message: Byte): Boolean =
		bytes.isNotEmpty() && bytes[0] == message

	private fun isChannelVoiceMessage(message: Byte): Boolean =
		bytes.isNotEmpty() && (bytes[0] and 0xF0.toByte() == message)
}