package com.stevenfrew.beatprompter.comm

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.Task

class ReceiverTask(val mName: String, private val mReceiver: Receiver) : Task(true) {
    override fun doWork() {
        try {
            mReceiver.receive()
        } catch (exception: Exception) {
            // Any I/O error means this receiver is dead to us.
            Log.e(BeatPrompterApplication.TAG_COMMS, "Unexpected IO exception from receiver.", exception)
            Log.d(BeatPrompterApplication.TAG_COMMS, "Receiver '$mName' threw an exception. Assuming it to be dead.")
            super.stop()
            Log.d(BeatPrompterApplication.TAG_COMMS, "Receiver is now stopped.")
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
}