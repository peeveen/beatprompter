package com.stevenfrew.beatprompter.comm.midi

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.SongDisplayActivity
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.midi.message.incoming.*

class DispatcherTask internal constructor() : Task(false) {
    override fun doWork() {
        try {
            while (!shouldStop) {
                val message = MIDIController.mMIDIInQueue.take()
                if (message is ControlChangeMessage) {
                    if(message.isMSBBankSelect())
                        EventHandler.sendEventToSongList(EventHandler.MIDI_MSB_BANK_SELECT, message.mChannel.toInt(), message.mValue.toInt())
                    else if(message.isLSBBankSelect())
                        EventHandler.sendEventToSongList(EventHandler.MIDI_LSB_BANK_SELECT, message.mChannel.toInt(), message.mValue.toInt())
                }
                else if(message is ProgramChangeMessage)
                    EventHandler.sendEventToSongList(EventHandler.MIDI_PROGRAM_CHANGE, message.mChannel.toInt(), message.mValue.toInt())
                else if(message is SongSelectMessage)
                    EventHandler.sendEventToSongList(EventHandler.MIDI_SONG_SELECT, message.mSong.toInt())
                else if(SongDisplayActivity.mSongDisplayActive)
                {
                    when (message) {
                        is StartMessage -> EventHandler.sendEventToSongDisplay(EventHandler.MIDI_START_SONG)
                        is ContinueMessage -> EventHandler.sendEventToSongDisplay(EventHandler.MIDI_CONTINUE_SONG)
                        is StopMessage -> EventHandler.sendEventToSongDisplay(EventHandler.MIDI_STOP_SONG)
                        is SongPositionPointerMessage -> EventHandler.sendEventToSongDisplay(EventHandler.MIDI_SET_SONG_POSITION, message.mMidiBeat, 0)
                    }
                }
            }
        } catch (ie: InterruptedException) {
            Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to dispatch MIDI message.", ie)
        }
    }
}