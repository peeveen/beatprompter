package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.comm.midi.message.OutgoingMessage

interface AliasComponent {
	fun resolve(aliases: List<Alias>, parameters: ByteArray, channel: Byte): List<OutgoingMessage>
}