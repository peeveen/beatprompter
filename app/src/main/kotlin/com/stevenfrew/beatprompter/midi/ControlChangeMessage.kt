package com.stevenfrew.beatprompter.midi

internal class ControlChangeMessage(controller: Byte, value: Byte, channel: Byte) : OutgoingMessage(Message.mergeMessageByteWithChannel(Message.MIDI_CONTROL_CHANGE_BYTE, channel), controller, value)