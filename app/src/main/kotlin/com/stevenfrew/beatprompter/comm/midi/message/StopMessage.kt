package com.stevenfrew.beatprompter.comm.midi.message

internal object StopMessage
    : OutgoingMessage(byteArrayOf(0x0f, MIDI_STOP_BYTE, 0, 0), true)