package com.stevenfrew.beatprompter.comm.bluetooth.message

/**
 * Bluetooth message that instructs the receiver to pause, because the band leader is scrolling
 * the display.
 */
object PauseOnScrollStartMessage
    : SignalOnlyMessage(BluetoothMessage.PAUSE_ON_SCROLL_START_MESSAGE_ID)
