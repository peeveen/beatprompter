package com.stevenfrew.beatprompter.midi

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication

internal class USBOutTask : USBTask(null, null, true) {
    override fun doWork() {
        try {
            while (!shouldStop)
            {
                val message = MIDIController.mMIDIOutQueue.take();
                if(message!=null) {
                    val connection = connection
                    val endpoint = endpoint
                    if (connection != null && endpoint != null)
                        connection.bulkTransfer(endpoint, message.mMessageBytes, message.mMessageBytes.size, 60000)
                }
            }
        } catch (ie: InterruptedException) {
            Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to retrieve MIDI out message.", ie)
        }

    }
}