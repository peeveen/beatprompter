package com.stevenfrew.beatprompter.comm.bluetooth.message

/**
 * Bluetooth message that instructs the receiver to stop playing the current song.
 */
class QuitSongMessage : SignalOnlyMessage(QUIT_SONG_MESSAGE_ID) {
    companion object {
        internal const val QUIT_SONG_MESSAGE_ID: Byte = 4
    }
}