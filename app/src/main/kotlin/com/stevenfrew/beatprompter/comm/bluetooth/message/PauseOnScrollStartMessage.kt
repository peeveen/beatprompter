package com.stevenfrew.beatprompter.comm.bluetooth.message

/**
 * Bluetooth message that instructs the receiver to pause, because the band leader is scrolling
 * the display.
 */
class PauseOnScrollStartMessage : SignalOnlyMessage(PAUSE_ON_SCROLL_START_MESSAGE_ID) {
    companion object {
        internal const val PAUSE_ON_SCROLL_START_MESSAGE_ID: Byte = 3
    }
}
