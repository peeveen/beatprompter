package com.stevenfrew.beatprompter.comm.midi.message

internal class ControlChangeMessage(controller: Byte,
                                    value: Byte,
                                    channel: Byte)
    : OutgoingMessage(mergeMessageByteWithChannel(MIDI_CONTROL_CHANGE_BYTE, channel), controller, value)