package com.stevenfrew.beatprompter.comm

import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class SenderTask constructor(private val mMessageQueue: MessageQueue)
    : Task(false), CoroutineScope {
    private val mCoRoutineJob = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + mCoRoutineJob

    override fun doWork() {
        try {
            // This take() will block if the queue is empty
            val messages = mMessageQueue.getMessages()
            val senders = getSenders()
            if (senders.isNotEmpty())
                senders.forEach {
                    try {
                        Logger.logComms("Sending messages to '$it.key' ($it.value.name).")
                        it.value.send(messages)
                    } catch (commException: Exception) {
                        // Problem with the I/O? This sender is now dead to us.
                        Logger.logComms("Sender threw an exception. Assuming it to be dead.")
                        removeSender(it.key)
                    }
                }
        } catch (interruptedException: InterruptedException) {
            // Must have been signalled to stop ... main Task loop will cater for this.
        }
    }

    private val mSenders = mutableMapOf<String, Sender>()
    private val mSendersLock = Any()

    fun addSender(id: String, sender: Sender) {
        synchronized(mSendersLock)
        {
            Logger.logComms("Adding new sender '$id' ($sender.name) to the collection")
            mSenders[id] = sender
        }
    }

    fun removeSender(id: String) {
        getSender(id)?.also {
            Logger.logComms("Removing sender '$id' from the collection")
            closeSender(it)
            Logger.logComms("Sender '$id' has been closed.")
            synchronized(mSendersLock)
            {
                mSenders.remove(id)
            }
            Logger.logComms("Sender '$id' is now dead ... notifying main activity for UI.")
            EventHandler.sendEventToSongList(EventHandler.CONNECTION_LOST, it.name)
        }
    }

    fun removeAll() {
        Logger.logComms("Removing ALL senders from the collection.")
        synchronized(mSendersLock)
        {
            mSenders.keys.forEach { removeSender(it) }
        }
    }

    private fun getSender(id: String): Sender? {
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