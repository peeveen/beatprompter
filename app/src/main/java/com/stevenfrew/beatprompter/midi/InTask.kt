package com.stevenfrew.beatprompter.midi

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.Task

class InTask internal constructor() : Task(false) {
    override fun doWork() {
        try {
            while (!shouldStop)
            {
                val message = MIDIController.mMIDISongListInQueue.take()
                if(message!=null) {
                    when {
                        message.isMSBBankSelect() -> EventHandler.sendEventToSongList(EventHandler.MIDI_MSB_BANK_SELECT, message.getMIDIChannel().toInt(), message.getBankSelectValue())
                        message.isLSBBankSelect() -> EventHandler.sendEventToSongList(EventHandler.MIDI_LSB_BANK_SELECT, message.getMIDIChannel().toInt(), message.getBankSelectValue())
                        message.isProgramChange() -> EventHandler.sendEventToSongList(EventHandler.MIDI_PROGRAM_CHANGE, message.getMIDIChannel().toInt(), message.getProgramChangeValue())
                        message.isSongSelect() -> EventHandler.sendEventToSongList(EventHandler.MIDI_SONG_SELECT, message.getSongSelectValue())
                    }
                }
            }
        } catch (ie: InterruptedException) {
            Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to retrieve MIDI in message.", ie)
        }

    }
}