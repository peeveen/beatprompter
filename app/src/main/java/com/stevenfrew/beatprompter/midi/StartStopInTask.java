package com.stevenfrew.beatprompter.midi;

import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.EventHandler;
import com.stevenfrew.beatprompter.Task;

public class StartStopInTask extends Task
{
    public StartStopInTask()
    {
        super(false);
    }

    @Override
    public void initialise()
    {
        Controller.mMIDISongDisplayInQueue.clear();
    }

    @Override
    public void doWork()
    {
        IncomingMessage message;
        try {
            while (((message = Controller.mMIDISongDisplayInQueue.take()) != null) && (!getShouldStop())) {
                if(message.isStart())
                    EventHandler.sendEventToSongDisplay(EventHandler.MIDI_START_SONG);
                else if(message.isContinue())
                    EventHandler.sendEventToSongDisplay(EventHandler.MIDI_CONTINUE_SONG);
                else if(message.isStop())
                    EventHandler.sendEventToSongDisplay(EventHandler.MIDI_STOP_SONG);
                else if(message.isSongPositionPointer())
                    EventHandler.sendEventToSongDisplay(EventHandler.MIDI_SET_SONG_POSITION,((IncomingSongPositionPointerMessage)message).getMIDIBeat(),0);
            }
        } catch (InterruptedException ie) {
            Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to retrieve MIDI in message.", ie);
        }
    }
}

