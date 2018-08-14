package com.stevenfrew.beatprompter.midi

import android.util.Log
import com.stevenfrew.beatprompter.Task

class SongDisplayInTask internal constructor() : Task(false) {
    override fun doWork() {
        try {
            while (MIDIController.mMIDISongDisplayInQueue.take() != null && !shouldStop) {
                Log.d(MIDIController.MIDI_TAG, "Discarding message intended for song display mode")
                // Do nothing. These messages aren't meant for this activity.
            }
        } catch (ie: InterruptedException) {
            Log.d(MIDIController.MIDI_TAG, "Interrupted while attempting to retrieve MIDI in message.", ie)
        }
    }
}