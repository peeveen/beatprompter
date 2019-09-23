package com.stevenfrew.beatprompter.comm

import java.util.concurrent.ArrayBlockingQueue

open class MessageQueue(capacity: Int) {
    protected val mBlockingQueue = ArrayBlockingQueue<OutgoingMessage>(capacity)
    // This prevents over-allocation of objects, which creates very slow garbage collection, which
    // disrupts timing-critical operations.
    private val mOutBuffer = mutableListOf<OutgoingMessage>()

    internal fun getMessages(): List<OutgoingMessage> {
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

    internal fun putMessage(message: OutgoingMessage) {
        synchronized(mBlockingQueue)
        {
            mBlockingQueue.put(message)
        }
    }

    internal fun putMessages(messages: List<OutgoingMessage>) {
        synchronized(mBlockingQueue)
        {
            for (f in messages.indices)
                mBlockingQueue.put(messages[f])
        }
    }
}