package com.stevenfrew.beatprompter.comm

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.Task

class ReceiverTask(private val mName:String,private val mReceiver:Receiver): Task(true) {
    override fun doWork() {
        while(!shouldStop) {
            try {
                mReceiver.receive()
            } catch (exception: Exception) {
                // Any I/O error means this receiver is dead to us.
                break
            }
        }
        Log.d(BeatPrompterApplication.TAG,"Receiver is dead, telling user")
        EventHandler.sendEventToSongList(EventHandler.CONNECTION_LOST,mName)
    }
}