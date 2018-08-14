package com.stevenfrew.beatprompter.midi

internal class StopMessage : OutgoingMessage(MIDI_STOP_SIGNAL_BYTES, true) {
    companion object {
        private val MIDI_STOP_SIGNAL_BYTES = byteArrayOf(0x0f, Message.MIDI_STOP_BYTE, 0, 0)
    }
}