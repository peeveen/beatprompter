package com.stevenfrew.beatprompter.midi

internal class ProgramChangeMessage(value: Int, channel: Int) : OutgoingMessage(Message.mergeMessageByteWithChannel(Message.MIDI_PROGRAM_CHANGE_BYTE, channel.toByte()), value.toByte())