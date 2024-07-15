package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.comm.midi.message.OutgoingMessage

/**
 * A simple sequence of MIDI bytes.
 */
class SimpleAliasComponent(
	private val mValues: List<Value>,
	private val mChannelValue: ChannelValue?
) : AliasComponent {
	override val parameterCount: Int
		get() = (mValues.maxOfOrNull { (it as? ArgumentValue)?.argumentIndex ?: 0 } ?: -1) + 1

	override fun resolve(
		aliases: List<Alias>,
		parameters: ByteArray,
		channel: Byte
	): List<OutgoingMessage> {
		return listOf(OutgoingMessage(mValues.map {
			it.resolve(parameters, mChannelValue?.mValue ?: channel)
		}.toByteArray()))
	}
}
