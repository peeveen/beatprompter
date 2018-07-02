package com.stevenfrew.beatprompter.midi;

import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.EventHandler;
import com.stevenfrew.beatprompter.Task;

public class InTask extends Task
{
    public InTask()
    {
        super(false);
    }
    public void doWork()
    {
        IncomingMessage message;
        try {
            while (((message = Controller.mMIDISongListInQueue.take()) != null) && (!getShouldStop())) {
                if(message.isMSBBankSelect())
                    EventHandler.sendEventToSongList(EventHandler.MIDI_MSB_BANK_SELECT,message.getMIDIChannel(),message.getBankSelectValue());
                else if(message.isLSBBankSelect())
                    EventHandler.sendEventToSongList(EventHandler.MIDI_LSB_BANK_SELECT,message.getMIDIChannel(),message.getBankSelectValue());
                else if(message.isProgramChange())
                    EventHandler.sendEventToSongList(EventHandler.MIDI_PROGRAM_CHANGE,message.getMIDIChannel(),message.getProgramChangeValue());
                else if(message.isSongSelect())
                    EventHandler.sendEventToSongList(EventHandler.MIDI_SONG_SELECT,message.getSongSelectValue());
            }
        } catch (InterruptedException ie) {
            Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to retrieve MIDI in message.", ie);
        }
    }
}


