package com.stevenfrew.beatprompter.comm.bluetooth.message

import com.stevenfrew.beatprompter.comm.OutgoingMessage

open class BluetoothMessage(bytes: ByteArray) : OutgoingMessage(bytes) {
    companion object {
        internal const val CHOOSE_SONG_MESSAGE_ID: Byte = 0
        internal const val TOGGLE_START_STOP_MESSAGE_ID: Byte = 1
        internal const val SET_SONG_TIME_MESSAGE_ID: Byte = 2
        internal const val PAUSE_ON_SCROLL_START_MESSAGE_ID: Byte = 3
        internal const val QUIT_SONG_MESSAGE_ID: Byte = 4
        internal const val HEARTBEAT_MESSAGE_ID: Byte = 5
    }
}