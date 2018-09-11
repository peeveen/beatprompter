package com.stevenfrew.beatprompter.comm

import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.Task
import kotlinx.coroutines.experimental.*
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue

class SenderTask(private val mOutQueue:ArrayBlockingQueue<OutgoingMessage>) : Task(false) {
    override fun doWork() {
        while (!shouldStop) {
            // This will block if the queue is empty
            try {
                val firstMessage = mOutQueue.take()
                val otherMessages = mOutQueue.toList()
                mOutQueue.clear()
                val senders = getSenders()
                if (senders.isNotEmpty())
                    senders.forEach {
                        launch {
                            try {
                                it.send(listOf(firstMessage))
                                it.send(otherMessages)
                            } catch (ioException: IOException) {
                                // Problem with the I/O? This sender is now dead to us.
                                EventHandler.sendEventToSongList(EventHandler.CONNECTION_LOST,it.name)
                                mSenders.remove(it)
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

    private val mSenders= mutableListOf<SenderBase>()
    private val mSendersLock=Any()

    fun addSender(sender:SenderBase)
    {
        synchronized(mSendersLock)
        {
            mSenders.add(sender)
        }
    }

    fun getSenders():List<SenderBase>
    {
        synchronized(mSendersLock)
        {
            return mSenders.toList()
        }
    }
}