package com.stevenfrew.beatprompter.comm

import com.stevenfrew.beatprompter.Logger

class ReceiverTasks {
    private val mReceiverThreads = mutableMapOf<String, Thread>()
    private val mReceiverTasks = mutableMapOf<String, ReceiverTask>()
    private val mReceiverThreadsLock = Any()

    fun addReceiver(id: String, name: String, receiver: Receiver) {
        synchronized(mReceiverThreadsLock)
        {
            Logger.logComms("Starting new receiver task '$id:' ($name)")
            mReceiverTasks[id] = ReceiverTask(name, receiver).also {
                mReceiverThreads[id] = Thread(it).also { th ->
                    th.start()
                }
            }
            Logger.logComms("Started new receiver task '$id:' ($name)")
        }
    }

    fun stopAndRemoveReceiver(id: String) {
        var receiverTask: ReceiverTask?
        var receiverThread: Thread?
        synchronized(mReceiverThreadsLock)
        {
            Logger.logComms("Removing receiver task '$id'")
            receiverTask = mReceiverTasks[id]
            receiverThread = mReceiverThreads[id]
            mReceiverTasks.remove(id)
            mReceiverThreads.remove(id)
            Logger.logComms("Removed receiver task '$id'")
        }
        Logger.logComms("Stopping receiver task '$id'")
        receiverTask?.setUnregistered()
        receiverTask?.stop()
        receiverThread?.apply {
            interrupt()
            join()
        }
        Logger.logComms("Stopped receiver task '$id'")
    }

    fun stopAll() {
        synchronized(mReceiverThreadsLock)
        {
            Logger.logComms("Stopping ALL receiver tasks")
            mReceiverThreads.keys.forEach {
                stopAndRemoveReceiver(it)
            }
            Logger.logComms("Stopped ALL receiver tasks")
        }
    }

    val taskCount: Int
        get() = synchronized(mReceiverThreadsLock) { mReceiverTasks.size }
}