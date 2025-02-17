package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage

class Alias(
	val name: String,
	private val components: List<AliasComponent>,
	val withMidiStart: Boolean = false,
	val withMidiContinue: Boolean = false,
	val withMidiStop: Boolean = false,
) : AliasComponent {
	override val parameterCount
		get() = components.maxOf { it.parameterCount }

	override fun resolve(
		sourceAliasSet: AliasSet,
		aliasSets: List<AliasSet>,
		arguments: ByteArray,
		channel: Byte
	): Pair<List<MidiMessage>, Set<AliasSet>> =
		components.map { it.resolve(sourceAliasSet, aliasSets, arguments, channel) }.let {
			it.flatMap { it.first } to it.flatMap { it.second }
				.toSet()
		}
}