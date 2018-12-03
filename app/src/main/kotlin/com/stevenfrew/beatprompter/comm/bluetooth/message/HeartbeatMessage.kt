package com.stevenfrew.beatprompter.comm.bluetooth.message

/**
 * Bluetooth message that simply determines whether a connection is still alive.
 */
object HeartbeatMessage : SignalOnlyMessage(HEARTBEAT_MESSAGE_ID)