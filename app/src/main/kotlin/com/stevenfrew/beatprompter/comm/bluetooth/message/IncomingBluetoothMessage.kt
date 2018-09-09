package com.stevenfrew.beatprompter.bluetooth.message

/**
 * A received Bluetooth message.
 */
internal data class IncomingBluetoothMessage constructor(val receivedMessage: BluetoothMessage, val messageLength:Int)