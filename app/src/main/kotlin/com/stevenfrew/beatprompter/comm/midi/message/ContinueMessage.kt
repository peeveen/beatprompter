package com.stevenfrew.beatprompter.comm.midi.message

@Suppress("unused")
internal object ContinueMessage
	: MidiMessage(byteArrayOf(MIDI_CONTINUE_BYTE))