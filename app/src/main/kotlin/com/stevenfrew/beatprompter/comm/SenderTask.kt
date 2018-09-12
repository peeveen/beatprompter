package com.stevenfrew.beatprompter.comm

import com.stevenfrew.beatprompter.EventHandler
import kotlinx.coroutines.experimental.*
import java.util.concurrent.ArrayBlockingQueue

class SenderTask(private val mOutQueue:ArrayBlockingQueue<OutgoingMessage>) : CommunicationTask<Sender>() {
    override fun doWork() {
        while (!shouldStop) {
            // This will block if the queue is empty
            try {
                val firstMessage = mOutQueue.take()
                val otherMessages = mOutQueue.toList()
                mOutQueue.clear()
                val senders = getCommunicators()
                if (senders.isNotEmpty())
                    senders.forEach {
                        launch {
                            try {
                                it.value.send(listOf(firstMessage))
                                it.value.send(otherMessages)
                            } catch (commException: Exception) {
                                // Problem with the I/O? This sender is now dead to us.
                                EventHandler.sendEventToSongList(EventHandler.CONNECTION_LOST,it.value.name)
                                removeCommunicator(it.key)
                            }
                        }
                    }
            }
            catch(interruptedException:InterruptedException)
            {
                break
            }
        }
    }
}