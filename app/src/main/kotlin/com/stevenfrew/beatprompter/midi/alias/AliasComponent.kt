package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage

interface AliasComponent {
	val parameterCount: Int
	fun resolve(aliases: List<Alias>, parameters: ByteArray, channel: Byte): List<MidiMessage>
}