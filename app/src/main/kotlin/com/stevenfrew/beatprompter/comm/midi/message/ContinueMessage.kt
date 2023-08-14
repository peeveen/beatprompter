package com.stevenfrew.beatprompter.comm.midi.message

@Suppress("unused")
internal object ContinueMessage
	: OutgoingMessage(byteArrayOf(0x0f, MIDI_CONTINUE_BYTE, 0, 0), true)