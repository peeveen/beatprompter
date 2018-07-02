package com.stevenfrew.beatprompter.midi;

import android.util.Log;

import com.stevenfrew.beatprompter.Task;

public class SongDisplayInTask extends Task
{
    SongDisplayInTask()
    {
        super(false);
    }
    public void doWork()
    {
        try {
            while ((MIDIController.mMIDISongDisplayInQueue.take() != null) && (!getShouldStop())) {
                Log.d(MIDIController.MIDI_TAG,"Discarding message intended for song display mode");
                // Do nothing. These messages aren't meant for this activity.
            }
        } catch (InterruptedException ie) {
            Log.d(MIDIController.MIDI_TAG, "Interrupted while attempting to retrieve MIDI in message.", ie);
        }
    }
}


