package com.stevenfrew.beatprompter.comm.bluetooth.message

import com.stevenfrew.beatprompter.comm.OutgoingMessage

/**
 * Base class for sent/received Bluetooth messages.
 */
abstract class Message(bytes:ByteArray):OutgoingMessage(bytes) {
    companion object {
        // Size of a "long", in bytes.
        internal const val LONG_BUFFER_SIZE = 8
    }
}