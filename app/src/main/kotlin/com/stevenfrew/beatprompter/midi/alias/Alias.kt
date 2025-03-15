package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage
import com.stevenfrew.beatprompter.midi.MidiTrigger

class Alias(
	val name: String,
	private val components: List<AliasComponent>,
	val triggers: List<MidiTrigger>,
	val withMidiStart: Boolean = false,
	val withMidiContinue: Boolean = false,
	val withMidiStop: Boolean = false,
	val commandName: String? = null
) : AliasComponent {
	override val parameterCount
		get() = components.maxOfOrNull { it.parameterCount } ?: 0

	override fun resolve(
		sourceAliasSet: AliasSet,
		aliases: List<AliasSet>,
		parameters: ByteArray,
		channel: Byte
	): Pair<List<MidiMessage>, Set<AliasSet>> =
		components.map { it.resolve(sourceAliasSet, aliases, parameters, channel) }.let {
			it.flatMap { it.first } to it.flatMap { it.second }
				.toSet()
		}
}