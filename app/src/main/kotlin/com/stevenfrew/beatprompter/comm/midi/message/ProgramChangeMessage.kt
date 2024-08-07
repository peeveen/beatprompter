package com.stevenfrew.beatprompter.comm.midi.message

internal class ProgramChangeMessage(
	value: Int,
	channel: Int
) : MidiMessage(
	mergeMessageByteWithChannel(MIDI_PROGRAM_CHANGE_BYTE, channel.toByte()),
	value.toByte()
)