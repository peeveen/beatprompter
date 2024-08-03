package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage

class Alias(
	val name: String,
	private val components: List<AliasComponent>
) {
	val parameterCount
		get() = components.maxOf { it.parameterCount }

	fun resolve(aliases: List<Alias>, arguments: ByteArray, channel: Byte): List<MidiMessage> =
		components.flatMap { it.resolve(aliases, arguments, channel) }
}