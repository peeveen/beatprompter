package com.stevenfrew.beatprompter.midi;

import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.Task;

public class MIDISongDisplayInTask extends Task
{
    public MIDISongDisplayInTask()
    {
        super(false);
    }
    public void doWork()
    {
        try {
            while ((BeatPrompterApplication.mMIDISongDisplayInQueue.take() != null) && (!getShouldStop())) {
                Log.d(BeatPrompterApplication.MIDI_TAG,"Discarding message intended for song display mode");
                // Do nothing. These messages aren't meant for this activity.
            }
        } catch (InterruptedException ie) {
            Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to retrieve MIDI in message.", ie);
        }
    }
}


