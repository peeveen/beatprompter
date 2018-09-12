package com.stevenfrew.beatprompter.comm

import com.stevenfrew.beatprompter.Task

abstract class CommunicationTask<TCommunicatorType>: Task(false) where TCommunicatorType:Communicator {

    private val mCommunicators= mutableMapOf<String,TCommunicatorType>()
    private val mCommunicatorsLock=Any()

    fun addCommunicator(id:String,sender: TCommunicatorType)
    {
        synchronized(mCommunicatorsLock)
        {
            mCommunicators[id]=sender
        }
    }

    fun removeCommunicator(id:String)
    {
        closeSender(getCommunicator(id))
        synchronized(mCommunicatorsLock)
        {
            mCommunicators.remove(id)
        }
    }

    private fun getCommunicator(id:String):TCommunicatorType?
    {
        synchronized(mCommunicatorsLock)
        {
            return mCommunicators[id]
        }
    }

    private fun closeSender(communicator:TCommunicatorType?)
    {
        try
        {
            communicator?.close()
        }
        catch(closeException:Exception)
        {
            // Couldn't close it? Who cares ...
        }
    }

    fun getCommunicators():Map<String,TCommunicatorType>
    {
        synchronized(mCommunicatorsLock)
        {
            return mCommunicators.toMap()
        }
    }

    val communicatorCount:Int
    get()=synchronized(mCommunicatorsLock)
        {
            mCommunicators.size
        }
}