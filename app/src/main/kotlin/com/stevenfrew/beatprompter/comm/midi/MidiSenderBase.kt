package com.stevenfrew.beatprompter.comm.midi

import com.stevenfrew.beatprompter.comm.CommunicationType
import com.stevenfrew.beatprompter.comm.MessageType
import com.stevenfrew.beatprompter.comm.SenderBase

abstract class MidiSenderBase(
	name: String,
	type: CommunicationType
) : SenderBase(name, type) {
	override val messageType: MessageType = MessageType.Midi
}