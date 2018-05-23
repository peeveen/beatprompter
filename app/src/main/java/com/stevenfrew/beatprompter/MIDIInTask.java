package com.stevenfrew.beatprompter;

import android.os.Handler;
import android.util.Log;

class MIDIInTask extends Task
{
    Handler mHandler;
    MIDIInTask(Handler handler)
    {
        super(false);
        mHandler=handler;
    }
    public void doWork()
    {
        MIDIIncomingMessage message;
        try {
            while (((message = BeatPrompterApplication.mMIDISongListInQueue.take()) != null) && (!getShouldStop())) {
                if(message.isMSBBankSelect())
                    mHandler.obtainMessage(BeatPrompterApplication.MIDI_MSB_BANK_SELECT,message.getMIDIChannel(),message.getBankSelectValue()).sendToTarget();
                else if(message.isLSBBankSelect())
                    mHandler.obtainMessage(BeatPrompterApplication.MIDI_LSB_BANK_SELECT,message.getMIDIChannel(),message.getBankSelectValue()).sendToTarget();
                else if(message.isProgramChange())
                    mHandler.obtainMessage(BeatPrompterApplication.MIDI_PROGRAM_CHANGE,message.getMIDIChannel(),message.getProgramChangeValue()).sendToTarget();
                else if(message.isSongSelect())
                    mHandler.obtainMessage(BeatPrompterApplication.MIDI_SONG_SELECT,message.getSongSelectValue()).sendToTarget();
            }
        } catch (InterruptedException ie) {
            Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to retrieve MIDI in message.", ie);
        }
    }
}


