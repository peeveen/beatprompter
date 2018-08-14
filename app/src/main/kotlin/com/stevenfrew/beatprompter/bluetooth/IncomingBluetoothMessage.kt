package com.stevenfrew.beatprompter.bluetooth

internal data class IncomingBluetoothMessage constructor(val receivedMessage:BluetoothMessage,val messageLength:Int)