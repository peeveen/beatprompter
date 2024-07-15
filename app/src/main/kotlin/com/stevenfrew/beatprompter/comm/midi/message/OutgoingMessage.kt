package com.stevenfrew.beatprompter.comm.midi.message

open class OutgoingMessage : Message {
	protected constructor(byte1: Byte, byte2: Byte) : this(
		byteArrayOf(
			getCodeIndex(byte1),
			byte1,
			byte2,
			ZERO_BYTE
		)
	)

	protected constructor(
		byte1: Byte,
		byte2: Byte,
		byte3: Byte
	) : this(byteArrayOf(getCodeIndex(byte1), byte1, byte2, byte3))

	protected constructor(
		bytes: ByteArray,
		codeIndexPresent: Boolean
	) : super(if (codeIndexPresent) padToFourBytes(bytes) else padToFourBytes(appendCodeIndex(bytes)))

	constructor(bytes: ByteArray) : super(padToFourBytes(appendCodeIndex(bytes)))

	companion object {
		private fun padToFourBytes(bytes: ByteArray): ByteArray {
			return if (bytes.size < 4) ByteArray(4) { _ -> ZERO_BYTE }.also {
				System.arraycopy(bytes, 0, it, 0, bytes.size)
			} else bytes
		}

		private fun appendCodeIndex(bytes: ByteArray): ByteArray {
			return ByteArray(bytes.size + 1).also {
				it[0] = getCodeIndex(bytes[0])
				System.arraycopy(bytes, 0, it, 1, bytes.size)
			}
		}

		private fun getCodeIndex(messageType: Byte): Byte {
			// TODO: support more messages.
			return ((messageType.toInt() shr 4) and 0x0F).toByte()
		}
	}
}