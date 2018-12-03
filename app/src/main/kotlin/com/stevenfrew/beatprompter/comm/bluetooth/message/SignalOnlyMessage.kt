package com.stevenfrew.beatprompter.comm.bluetooth.message

/**
 * Base class for Bluetooth messages that are "signal only", i.e. they have no accompanying data.
 */
abstract class SignalOnlyMessage constructor(messageID: Byte) : BluetoothMessage(byteArrayOf(messageID))