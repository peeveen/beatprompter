package com.stevenfrew.beatprompter.comm

open class OutgoingMessage(val mBytes:ByteArray) {
    val length:Int
        get() = mBytes.size

    override fun toString(): String {
        val strFormat = StringBuilder()
        for (mMessageByte in mBytes) strFormat.append(String.format("%02X ", mMessageByte))
        return strFormat.toString()
    }
}