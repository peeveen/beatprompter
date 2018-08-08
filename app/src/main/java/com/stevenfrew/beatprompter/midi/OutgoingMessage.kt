package com.stevenfrew.beatprompter.midi

open class OutgoingMessage : Message {
    protected constructor(byte1: Byte, byte2: Byte) : this(byteArrayOf(getCodeIndex(byte1), byte1, byte2, 0.toByte())) {}
    protected constructor(byte1: Byte, byte2: Byte, byte3: Byte) : this(byteArrayOf(getCodeIndex(byte1), byte1, byte2, byte3)) {}
    protected constructor(bytes: ByteArray, codeIndexPresent: Boolean) : super(if (codeIndexPresent) bytes else appendCodeIndex(bytes)) {}
    constructor(bytes: ByteArray) : super(appendCodeIndex(bytes))

    companion object {
        private fun appendCodeIndex(bytes: ByteArray): ByteArray {
            val newBytes = ByteArray(bytes.size + 1)
            newBytes[0] = getCodeIndex(bytes[0])
            System.arraycopy(bytes, 0, newBytes, 1, bytes.size)
            return newBytes
        }

        private fun getCodeIndex(messageType: Byte): Byte {
            // TODO: support more messages.
            return ((messageType.toInt() shr 4) and 0x0F).toByte()
        }
    }
}