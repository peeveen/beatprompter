package com.stevenfrew.beatprompter.comm.midi.message

internal class SongSelectMessage(song: Int) :
	OutgoingMessage(MIDI_SONG_SELECT_BYTE, (song and 0x7F).toByte())