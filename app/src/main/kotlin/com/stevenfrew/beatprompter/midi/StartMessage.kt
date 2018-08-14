package com.stevenfrew.beatprompter.midi

internal class StartMessage : OutgoingMessage(MIDI_START_SIGNAL_BYTES, true) {
    companion object {
        private val MIDI_START_SIGNAL_BYTES = byteArrayOf(0x0f, Message.MIDI_START_BYTE, 0, 0)
    }
}