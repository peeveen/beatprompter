package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.comm.midi.message.OutgoingMessage

class Alias(
	name: String,
	components: List<AliasComponent>
) {
	val mName = name
	val parameterCount
		get() = mComponents.maxOf { it.parameterCount }
	private val mComponents = components

	fun resolve(aliases: List<Alias>, arguments: ByteArray, channel: Byte): List<OutgoingMessage> =
		mComponents.flatMap { it.resolve(aliases, arguments, channel) }
}