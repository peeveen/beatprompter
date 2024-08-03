package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage

/**
 * A simple sequence of MIDI bytes.
 */
class SimpleAliasComponent(
	private val values: List<Value>,
	private val channelValue: ChannelValue?
) : AliasComponent {
	override val parameterCount: Int
		get() = (values.maxOfOrNull { (it as? ArgumentValue)?.argumentIndex ?: -1 } ?: -1) + 1

	override fun resolve(
		aliases: List<Alias>,
		parameters: ByteArray,
		channel: Byte
	): List<MidiMessage> =
		listOf(MidiMessage(values.map {
			it.resolve(parameters, channelValue?.value ?: channel)
		}.toByteArray()))
}
