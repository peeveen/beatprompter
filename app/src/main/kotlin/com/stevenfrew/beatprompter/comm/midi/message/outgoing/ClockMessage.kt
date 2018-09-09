package com.stevenfrew.beatprompter.comm.midi.message.outgoing

internal class ClockMessage : OutgoingMessage(MIDI_CLOCK_SIGNAL_BYTES, true) {
    companion object {
        private val MIDI_CLOCK_SIGNAL_BYTES = byteArrayOf(0x0f, 0xf8.toByte(), 0, 0)
    }
}