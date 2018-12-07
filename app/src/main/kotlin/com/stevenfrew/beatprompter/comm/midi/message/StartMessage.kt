package com.stevenfrew.beatprompter.comm.midi.message

internal object StartMessage
    : OutgoingMessage(byteArrayOf(0x0f, MIDI_START_BYTE, 0, 0), true)