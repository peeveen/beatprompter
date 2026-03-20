package com.stevenfrew.beatprompter.comm.midi

import com.stevenfrew.beatprompter.comm.CommunicationType
import com.stevenfrew.beatprompter.comm.MessageType
import com.stevenfrew.beatprompter.comm.SenderBase
import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage

abstract class MidiSenderBase(
	name: String,
	type: CommunicationType
) : SenderBase<MidiMessage>(name, type, MessageType.Midi)