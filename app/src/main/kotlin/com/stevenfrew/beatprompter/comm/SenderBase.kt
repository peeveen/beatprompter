package com.stevenfrew.beatprompter.comm

abstract class SenderBase constructor(private val mName: String,
                                      private val mBufferSize: Int = OUT_BUFFER_SIZE)
    : Sender {
    private val mOutBuffer = ByteArray(mBufferSize)

    override val name: String
        get() = mName

    override fun send(messages: List<OutgoingMessage>) {
        var currentMessageIndex = 0
        while (currentMessageIndex < messages.size) {
            var bytesCopied = 0
            // Copy messages to out-buffer
            // We might have more than 4K of data to send here, so might need to loop.
            while (currentMessageIndex < messages.size) {
                val messageSize = messages[currentMessageIndex].length
                if (bytesCopied + messageSize > mBufferSize)
                    break
                System.arraycopy(messages[currentMessageIndex].mBytes,
                        0, mOutBuffer, bytesCopied, messageSize)
                bytesCopied += messageSize
                ++currentMessageIndex
            }
            sendMessageData(mOutBuffer, bytesCopied)
        }
    }

    protected abstract fun sendMessageData(bytes: ByteArray, length: Int)

    companion object {
        // 4K buffer by default.
        private const val OUT_BUFFER_SIZE = 4096
    }
}