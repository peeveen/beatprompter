package com.stevenfrew.beatprompter.comm

import java.util.concurrent.ArrayBlockingQueue

class MessageQueue(capacity: Int) {
    private val mBlockingQueue = ArrayBlockingQueue<OutgoingMessage>(capacity)

    fun getMessages(): List<OutgoingMessage> {
        // This take() will cause a block if empty
        val firstMessage = listOf(mBlockingQueue.take())
        synchronized(mBlockingQueue)
        {
            val otherMessages = mBlockingQueue.toList()
            mBlockingQueue.clear()
            return if (otherMessages.isEmpty())
                firstMessage
            else
                listOf(firstMessage, otherMessages).flatten()
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
            mBlockingQueue.addAll(messages)
        }
    }
}