package com.stevenfrew.beatprompter.comm

import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.Task

class ReceiverTask(val mName: String,
                   private val mReceiver: Receiver)
    : Task(true) {
    private var mUnregistered = false

    override fun doWork() {
        try {
            mReceiver.receive()
        } catch (exception: Exception) {
            // Any I/O error means this receiver is dead to us.
            Logger.logComms("Unexpected IO exception from receiver.", exception)
            Logger.logComms("Receiver '$mName' threw an exception. Assuming it to be dead.")
            if (!mUnregistered)
                mReceiver.unregister(this)
            super.stop()
            Logger.logComms("Receiver is now stopped.")
            EventHandler.sendEventToSongList(EventHandler.CONNECTION_LOST, mName)
        }
    }

    override fun stop() {
//        super.stop()
        // Receivers often block when trying to receive data, so closing the socket or whatever behind
        // the scenes will usually kickstart it into action.
        try {
            mReceiver.close()
        } catch (exception: Exception) {
            // At least we tried ...
        }
    }

    fun setUnregistered() {
        mUnregistered = true
    }
}