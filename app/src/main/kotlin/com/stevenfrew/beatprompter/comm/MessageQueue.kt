package com.stevenfrew.beatprompter.comm

import java.util.concurrent.ArrayBlockingQueue

class MessageQueue(capacity: Int) {
    private val mBlockingQueue = ArrayBlockingQueue<OutgoingMessage>(capacity)
    // This prevents over-allocation of objects, which creates very slow garbage collection, which
    // disrupts timing critical operation.s
    private val mOutBuffer = mutableListOf<OutgoingMessage>()

    fun getMessages(): List<OutgoingMessage> {
        mOutBuffer.clear()
        // This take() will cause a block if empty
        mOutBuffer.add(mBlockingQueue.take())
        synchronized(mBlockingQueue)
        {
            while (mBlockingQueue.isNotEmpty())
                mOutBuffer.add(mBlockingQueue.remove())
            return mOutBuffer
        }
    }

    fun putMessage(message: OutgoingMessage) {
        synchronized(mBlockingQueue)
        {
            mBlockingQueue.put(message)
        }
    }

    fun putMessages(messages: List<OutgoingMessage>) {
        synchronized(mBlockingQueue)
        {
            for (f in 0 until messages.size)
                mBlockingQueue.put(messages[f])
        }
    }
}