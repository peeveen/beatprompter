package com.stevenfrew.beatprompter.midi

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.Task

class StartStopInTask : Task(false) {

    public override fun initialise() {
        MIDIController.mMIDISongDisplayInQueue.clear()
    }

    override fun doWork() {
        try {
            while (!shouldStop) {
                val message = MIDIController.mMIDISongDisplayInQueue.take()
                if(message!=null) {
                    if (message.isStart())
                        EventHandler.sendEventToSongDisplay(EventHandler.MIDI_START_SONG)
                    else if (message.isContinue())
                        EventHandler.sendEventToSongDisplay(EventHandler.MIDI_CONTINUE_SONG)
                    else if (message.isStop())
                        EventHandler.sendEventToSongDisplay(EventHandler.MIDI_STOP_SONG)
                    else if (message.isSongPositionPointer())
                        EventHandler.sendEventToSongDisplay(EventHandler.MIDI_SET_SONG_POSITION, (message as IncomingSongPositionPointerMessage).midiBeat, 0)
                }
            }
        } catch (ie: InterruptedException) {
            Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to retrieve MIDI in message.", ie)
        }

    }
}