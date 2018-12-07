package com.stevenfrew.beatprompter.comm

abstract class SenderBase constructor(private val mName: String)
    : Sender {
    override val name: String
        get() = mName

    override fun send(message: OutgoingMessage) {
        sendMessageData(message.mBytes, message.length)
    }

    protected abstract fun sendMessageData(bytes: ByteArray, length: Int)
}