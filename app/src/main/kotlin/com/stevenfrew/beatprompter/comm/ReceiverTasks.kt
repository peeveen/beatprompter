package com.stevenfrew.beatprompter.comm

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication

class ReceiverTasks {
    private val mReceiverThreads = mutableMapOf<String, Thread>()
    private val mReceiverTasks = mutableMapOf<String, ReceiverTask>()
    private val mReceiverThreadsLock = Any()

    fun addReceiver(id: String, name: String, receiver: Receiver) {
        synchronized(mReceiverThreadsLock)
        {
            Log.d(BeatPrompterApplication.TAG_COMMS, "Starting new receiver task '$id:' ($name)")
            mReceiverTasks[id] = ReceiverTask(name, receiver).also {
                mReceiverThreads[id] = Thread(it).also { th ->
                    th.start()
                }
            }
            Log.d(BeatPrompterApplication.TAG_COMMS, "Started new receiver task '$id:' ($name)")
        }
    }

    fun stopAndRemoveReceiver(id: String) {
        var receiverTask: ReceiverTask?
        var receiverThread: Thread?
        synchronized(mReceiverThreadsLock)
        {
            Log.d(BeatPrompterApplication.TAG_COMMS, "Removing receiver task '$id'")
            receiverTask = mReceiverTasks[id]
            receiverThread = mReceiverThreads[id]
            mReceiverTasks.remove(id)
            mReceiverThreads.remove(id)
            Log.d(BeatPrompterApplication.TAG_COMMS, "Removed receiver task '$id'")
        }
        Log.d(BeatPrompterApplication.TAG_COMMS, "Stopping receiver task '$id'")
        receiverTask?.setUnregistered()
        receiverTask?.stop()
        receiverThread?.apply {
            interrupt()
            join()
        }
        Log.d(BeatPrompterApplication.TAG_COMMS, "Stopped receiver task '$id'")
    }

    fun stopAll() {
        synchronized(mReceiverThreadsLock)
        {
            Log.d(BeatPrompterApplication.TAG_COMMS, "Stopping ALL receiver tasks")
            mReceiverThreads.keys.forEach {
                stopAndRemoveReceiver(it)
            }
            Log.d(BeatPrompterApplication.TAG_COMMS, "Stopped ALL receiver tasks")
        }
    }

    val taskCount: Int
        get() = synchronized(mReceiverThreadsLock) { mReceiverTasks.size }
}