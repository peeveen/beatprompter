package com.stevenfrew.beatprompter.comm

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.Task

class ReceiverTasks {
    private val mReceiverThreads=mutableMapOf<String,Thread>()
    private val mReceiverTasks=mutableMapOf<String, Task>()
    private val mReceiverThreadsLock=Any()

    fun addReceiver(id:String,name:String,receiver:Receiver)
    {
        synchronized(mReceiverThreadsLock)
        {
            Log.d(BeatPrompterApplication.TAG,"Starting new receiver task '$id:' ($name)")
            mReceiverTasks[id]=ReceiverTask(name,receiver).also {
                mReceiverThreads[id]=Thread(it).also{th->
                    th.start()
                }
            }
        }
    }

    fun removeReceiver(id:String)
    {
        synchronized(mReceiverThreadsLock)
        {
            Log.d(BeatPrompterApplication.TAG,"Stopping receiver task '$id'")
            mReceiverTasks[id]?.stop()
            mReceiverThreads[id]?.apply {
                interrupt()
                join()
            }
            mReceiverTasks.remove(id)
            mReceiverThreads.remove(id)
        }
    }

    fun stopAll()
    {
        synchronized(mReceiverThreadsLock)
        {
            Log.d(BeatPrompterApplication.TAG,"Stopping ALL receiver tasks")
            mReceiverThreads.keys.forEach {
                removeReceiver(it)
            }
        }
    }

    val taskCount:Int
        get()= synchronized(mReceiverThreadsLock){mReceiverTasks.size}
}