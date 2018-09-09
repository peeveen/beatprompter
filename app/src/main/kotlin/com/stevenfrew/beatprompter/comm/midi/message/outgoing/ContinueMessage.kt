package com.stevenfrew.beatprompter.comm.midi.message.outgoing

internal class ContinueMessage : OutgoingMessage(MIDI_CONTINUE_SIGNAL_BYTES, true) {
    companion object {
        private val MIDI_CONTINUE_SIGNAL_BYTES = byteArrayOf(0x0f, MIDI_CONTINUE_BYTE, 0, 0)
    }
}