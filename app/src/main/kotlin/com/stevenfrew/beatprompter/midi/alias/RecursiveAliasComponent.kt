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
		sourceAliasSet: AliasSet,
		aliasSets: List<AliasSet>,
		parameters: ByteArray,
		channel: Byte
	): Pair<List<MidiMessage>, Set<AliasSet>> {
		val (alias, set) = aliasSets.firstNotNullOfOrNull { set ->
			set.aliases.firstOrNull {
				it.name.equals(
					referencedAliasName,
					ignoreCase = true
				) && it.parameterCount == arguments.size
			}?.let {
				it to set
			}
		} ?: throw ResolutionException(
			BeatPrompter.appResources.getString(
				R.string.unknown_midi_directive,
				referencedAliasName
			)
		)
		val (midiMessages, usedSets) = alias.resolve(set, aliasSets, arguments.map {
			it.resolve(parameters, channelValue?.value ?: channel)
		}.toByteArray(), channelValue?.value ?: channel)
		return midiMessages to mutableSetOf(sourceAliasSet, *usedSets.toTypedArray())
	}
}