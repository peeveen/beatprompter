package com.stevenfrew.beatprompter.comm.midi.message

internal object ClockMessage
    : OutgoingMessage(byteArrayOf(0x0f, 0xf8.toByte(), 0, 0), true)