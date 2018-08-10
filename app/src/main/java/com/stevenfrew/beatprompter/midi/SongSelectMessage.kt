package com.stevenfrew.beatprompter.midi

internal class SongSelectMessage(song: Int) : OutgoingMessage(Message.MIDI_SONG_SELECT_BYTE, (song and 0x7F).toByte())