package com.stevenfrew.beatprompter.comm.midi.message.outgoing

internal class ProgramChangeMessage(value: Int, channel: Int) : OutgoingMessage(mergeMessageByteWithChannel(MIDI_PROGRAM_CHANGE_BYTE, channel.toByte()), value.toByte())