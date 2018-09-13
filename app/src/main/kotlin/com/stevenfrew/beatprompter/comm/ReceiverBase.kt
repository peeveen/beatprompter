package com.stevenfrew.beatprompter.comm

abstract class ReceiverBase constructor(private val mName:String,private val mBufferSize:Int=IN_BUFFER_SIZE):Receiver {
    private val mInBuffer=ByteArray(mBufferSize)

    override val name:String
        get()=mName

    override fun receive() {
        var offset=0
        while(true)
        {
            val amountReceived=receiveMessageData(mInBuffer,offset,mBufferSize-offset)
            if(amountReceived<=0)
                break
            val amountOfDataToParse=offset+amountReceived
            val amountOfDataParsed=parseMessageData(mInBuffer, offset, amountOfDataToParse)
            offset=amountOfDataToParse-amountOfDataParsed
        }
    }

    protected abstract fun receiveMessageData(buffer:ByteArray,offset:Int,maximumAmount:Int):Int

    protected abstract fun parseMessageData(buffer:ByteArray,dataStart:Int,dataEnd:Int):Int

    companion object {
        // 4K buffer by default.
        private const val IN_BUFFER_SIZE=4096
    }
}