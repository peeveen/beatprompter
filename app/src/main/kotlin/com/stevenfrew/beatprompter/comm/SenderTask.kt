package com.stevenfrew.beatprompter.comm

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.Task
import kotlinx.coroutines.experimental.*
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
                                synchronized(it.value.lock)
                                {
                                    it.value.send(listOf(firstMessage))
                                    it.value.send(otherMessages)
                                }
                            } catch (commException: Exception) {
                                // Problem with the I/O? This sender is now dead to us.
                                removeSender(it.key)
                            }
                        }
                    }
            }
            catch(interruptedException:InterruptedException)
            {
                break
            }
        }
        removeAll()
    }

    private val mSenders = mutableMapOf<String, Sender>()
    private val mSendersLock = Any()

    fun addSender(id: String, sender: Sender) {
        synchronized(mSendersLock)
        {
            mSenders[id] = sender
        }
    }

    fun removeSender(id: String) {
        getCommunicator(id)?.also {
            closeSender(it)
            synchronized(mSendersLock)
            {
                mSenders.remove(id)
            }
            Log.d(BeatPrompterApplication.TAG,"Sender is dead, telling user")
            EventHandler.sendEventToSongList(EventHandler.CONNECTION_LOST, it.name)
        }
    }

    fun removeAll()
    {
        synchronized(mSendersLock)
        {
            mSenders.keys.forEach{removeSender(it)}
        }
    }

    private fun getCommunicator(id: String): Sender? {
        synchronized(mSendersLock)
        {
            return mSenders[id]
        }
    }

    private fun closeSender(sender: Sender?) {
        try {
            sender?.close()
        } catch (closeException: Exception) {
            // Couldn't close it? Who cares ...
        }
    }

    private fun getSenders(): Map<String, Sender> {
        synchronized(mSendersLock)
        {
            return mSenders.toMap()
        }
    }

    val senderCount: Int
        get() = synchronized(mSendersLock)
        {
            mSenders.size
        }
}