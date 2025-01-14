package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage

class RecursiveAliasComponent(
	private val referencedAliasName: String,
	private val arguments: List<Value>,
	private val channelValue: ChannelValue?
) : AliasComponent {
	override val parameterCount: Int
		get() = (arguments.maxOfOrNull { (it as? ArgumentValue)?.argumentIndex ?: -1 } ?: -1) + 1

	override fun resolve(
		aliases: List<Alias>,
		parameters: ByteArray,
		channel: Byte
	): List<MidiMessage> =
		try {
			aliases.first {
				it.name.equals(
					referencedAliasName,
					ignoreCase = true
				) && it.parameterCount == arguments.size
			}
				.resolve(aliases, arguments.map {
					it.resolve(parameters, channelValue?.value ?: channel)
				}.toByteArray(), channelValue?.value ?: channel)
		} catch (_: NoSuchElementException) {
			throw ResolutionException(
				BeatPrompter.appResources.getString(
					R.string.unknown_midi_directive,
					referencedAliasName
				)
			)
		}
}
