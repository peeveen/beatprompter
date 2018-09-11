package com.stevenfrew.beatprompter.comm

import com.stevenfrew.beatprompter.Task
import kotlinx.coroutines.experimental.launch
import java.io.IOException

class ReceiverTask : Task(false) {
    override fun doWork() {
        while (!shouldStop) {
            try {
                val receivers = getReceivers()
                if (receivers.isNotEmpty())
                    receivers.forEach {
                        launch {
                            try {
                                it.receive()
                            } catch (ioException: IOException) {
                                // Problem with the I/O. This receiver is now dead to us.
                                mReceivers.remove(it)
                            }
                        }
                    }
                else
                    Thread.sleep(250)
            }
            catch(interruptedException:InterruptedException)
            {
                break
            }
        }
    }

    private val mReceivers=mutableListOf<Receiver>()
    private val mReceiversLock=Any()

    fun addReceiver(receiver: Receiver)
    {
        synchronized(mReceiversLock)
        {
            mReceivers.add(receiver)
        }
    }

    fun getReceivers():List<Receiver>
    {
        synchronized(mReceiversLock)
        {
            return mReceivers.toList()
        }
    }
}