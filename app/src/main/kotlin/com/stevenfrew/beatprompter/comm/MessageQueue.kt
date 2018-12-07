package com.stevenfrew.beatprompter.comm

import java.util.concurrent.ArrayBlockingQueue

class MessageQueue(capacity: Int) {
    private val mBlockingQueue = ArrayBlockingQueue<OutgoingMessage>(capacity)

    fun getMessage(): OutgoingMessage {
        // This take() will cause a block if empty
        return mBlockingQueue.take()
    }

    fun putMessage(message: OutgoingMessage) {
        synchronized(mBlockingQueue)
        {
            mBlockingQueue.put(message)
        }
    }
}