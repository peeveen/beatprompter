package com.stevenfrew.beatprompter.comm

abstract class ReceiverBase<TMessageType> constructor(private val mBufferSize:Int=IN_BUFFER_SIZE):Receiver<TMessageType> {
    private val mInBuffer=ByteArray(mBufferSize)

    override fun receive(): List<TMessageType> {
        var offset=0
        val messagesReceived=mutableListOf<TMessageType>()
        while(true)
        {
            val amountReceived=receiveMessageData(mInBuffer,offset,mBufferSize-offset)
            if(amountReceived==0)
                break
            val amountOfDataToParse=offset+amountReceived
            val parseResults=parseMessageData(mInBuffer, offset, amountOfDataToParse)
            val messagesParsed=parseResults.first
            val amountOfDataParsed=parseResults.second
            offset=amountOfDataToParse-amountOfDataParsed
            messagesReceived.addAll(messagesParsed)
        }
        return messagesReceived
    }

    protected abstract fun receiveMessageData(buffer:ByteArray,offset:Int,maximumAmount:Int):Int

    protected abstract fun parseMessageData(buffer:ByteArray,dataStart:Int,dataEnd:Int):Pair<List<TMessageType>,Int>

    companion object {
        // 4K buffer by default.
        private const val IN_BUFFER_SIZE=4096
    }
}