package com.stevenfrew.beatprompter.comm.midi.message

internal class SongPositionPointerMessage(
	midiBeats: Int,
	channel: Int
) : MidiMessage(
	mergeMessageByteWithChannel(MIDI_SONG_POSITION_POINTER_BYTE, channel.toByte()),
	((midiBeats shr 8) and 0x7f).toByte(),
	(midiBeats and 0x7f).toByte()
)