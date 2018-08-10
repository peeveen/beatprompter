package com.stevenfrew.beatprompter.bluetooth

class QuitSongMessage : SignalOnlyBluetoothMessage(QUIT_SONG_MESSAGE_ID) {
    companion object {
        internal const val QUIT_SONG_MESSAGE_ID: Byte = 4
    }
}