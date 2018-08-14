package com.stevenfrew.beatprompter.bluetooth

class PauseOnScrollStartMessage : SignalOnlyBluetoothMessage(PAUSE_ON_SCROLL_START_MESSAGE_ID) {
    companion object {
        internal const val PAUSE_ON_SCROLL_START_MESSAGE_ID: Byte = 3
    }
}
