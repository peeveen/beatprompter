package com.stevenfrew.beatprompter.comm.midi

import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.SenderBase
import com.stevenfrew.beatprompter.comm.midi.message.outgoing.OutgoingMessage
import kotlinx.coroutines.experimental.*
import java.io.IOException

class SenderTask : Task(false) {
    override fun doWork() {
        while (!shouldStop) {
            val senders=getSenders()
            val messages=MIDIController.mMIDIOutQueue.toList()
            MIDIController.mMIDIOutQueue.clear()
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

    companion object {
        private val mSenders= mutableListOf<SenderBase<OutgoingMessage>>()
        private val mSendersLock=Any()

        fun addSender(sender:SenderBase<OutgoingMessage>)
        {
            synchronized(mSendersLock)
            {
                mSenders.add(sender)
            }
        }

        fun getSenders():List<SenderBase<OutgoingMessage>>
        {
            synchronized(mSendersLock)
            {
                return mSenders.toList()
            }
        }
    }
}