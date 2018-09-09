package com.stevenfrew.beatprompter.bluetooth.message

/**
 * Bluetooth message that instructs the receiver to stop playing the current song.
 */
class QuitSongMessage : SignalOnlyBluetoothMessage(QUIT_SONG_MESSAGE_ID) {
    companion object {
        internal const val QUIT_SONG_MESSAGE_ID: Byte = 4
    }
}