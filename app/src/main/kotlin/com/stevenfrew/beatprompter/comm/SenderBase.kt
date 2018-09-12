package com.stevenfrew.beatprompter.comm

abstract class SenderBase constructor(private val mName:String,private val mBufferSize:Int=OUT_BUFFER_SIZE): Sender {
    private val mOutBuffer=ByteArray(mBufferSize)

    override val lock=Any()

    override val name:String
        get()=mName

    override suspend fun send(messages: List<OutgoingMessage>) {
        var messagesCopy=messages
        while(messagesCopy.isNotEmpty())
        {
            var messagesSent=0
            var bytesCopied=0
            // Copy messages to out-buffer
            // We might have more than 4K of data to send here, so might need to loop.
            for(message in messagesCopy)
            {
                val messageSize=message.length
                if(bytesCopied+messageSize>mBufferSize)
                    break
                System.arraycopy(message.mBytes, 0, mOutBuffer, bytesCopied, messageSize)
                bytesCopied += messageSize
                messagesSent++
            }
            sendMessageData(mOutBuffer,bytesCopied)
            messagesCopy=messagesCopy.drop(messagesSent)
        }
    }

    protected abstract fun sendMessageData(bytes:ByteArray,length:Int)

    companion object {
        // 4K buffer by default.
        private const val OUT_BUFFER_SIZE=4096
    }
}