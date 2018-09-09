package com.stevenfrew.beatprompter.bluetooth.message

/**
 * Base class for Bluetooth messages that are "signal only", i.e. they have no accompanying data.
 */
abstract class SignalOnlyBluetoothMessage constructor(messageID: Byte) : BluetoothMessage() {
    private val mMessageID=messageID

    override val bytes: ByteArray
        get() = byteArrayOf(mMessageID)
}
