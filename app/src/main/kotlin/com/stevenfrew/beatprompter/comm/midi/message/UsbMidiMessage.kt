package com.stevenfrew.beatprompter.comm.midi.message

import com.stevenfrew.beatprompter.comm.Message

open class UsbMidiMessage : MidiMessage {
	constructor(message: Message) : this(message.bytes)

	protected constructor(
		bytes: ByteArray,
		codeIndexPresent: Boolean
	) : super(padToFourBytes(if (codeIndexPresent) bytes else appendCodeIndex(bytes)))

	constructor(bytes: ByteArray) : this(bytes, false)

	companion object {
		private fun padToFourBytes(bytes: ByteArray): ByteArray =
			if (bytes.size < 4) ByteArray(4) { index -> if (index < bytes.size) bytes[index] else 0 } else bytes

		private fun appendCodeIndex(bytes: ByteArray): ByteArray =
			ByteArray(bytes.size + 1).also {
				it[0] = getCodeIndex(bytes[0])
				System.arraycopy(bytes, 0, it, 1, bytes.size)
			}

		private fun getCodeIndex(messageType: Byte): Byte =
			// TODO: support more messages.
			((messageType.toInt() shr 4) and 0x0F).toByte()
	}
}