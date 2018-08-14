package com.stevenfrew.beatprompter.bluetooth

abstract class SignalOnlyBluetoothMessage constructor(messageID: Byte) : BluetoothMessage() {
    private val mMessageID=messageID

    override val bytes: ByteArray?
        get() = byteArrayOf(mMessageID)
}
