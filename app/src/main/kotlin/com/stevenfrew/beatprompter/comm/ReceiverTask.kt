package com.stevenfrew.beatprompter.comm

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.Task

class ReceiverTask(private val mName:String,private val mReceiver:Receiver): Task(true) {
    override fun doWork() {
        while(!shouldStop) {
            try {
                Log.d(BeatPrompterApplication.TAG,"Asking receiver '$mName' for data ...")
                mReceiver.receive()
            } catch (exception: Exception) {
                // Any I/O error means this receiver is dead to us.
                Log.d(BeatPrompterApplication.TAG,"Receiver '$mName' threw an exception. Assuming it to be dead.")
                break
            }
        }
        Log.d(BeatPrompterApplication.TAG,"Receiver is now dead ... notifying main activity for UI.")
        EventHandler.sendEventToSongList(EventHandler.CONNECTION_LOST,mName)
    }
}