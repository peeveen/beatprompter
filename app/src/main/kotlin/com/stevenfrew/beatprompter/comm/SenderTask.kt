package com.stevenfrew.beatprompter.comm

import com.stevenfrew.beatprompter.Task
import kotlinx.coroutines.experimental.*
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue

class SenderTask(private val mOutQueue:ArrayBlockingQueue<OutgoingMessage>) : Task(false) {
    override fun doWork() {
        while (!shouldStop) {
            val senders= getSenders()
            val messages= mOutQueue.toList()
            mOutQueue.clear()
            if(senders.isNotEmpty())
                senders.forEach {
                    launch {
                        try {
                            it.send(messages)
                        }
                        catch(ioException: IOException)
                        {
                            // Problem with the I/O. This sender is now dead to us.
                            mSenders.remove(it)
                        }
                    }
                }
            else
                Thread.sleep(250)
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