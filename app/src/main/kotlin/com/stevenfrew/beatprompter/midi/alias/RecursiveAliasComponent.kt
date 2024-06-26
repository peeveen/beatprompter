package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.comm.midi.message.OutgoingMessage

class RecursiveAliasComponent(
	private val mReferencedAliasName: String,
	private val mArguments: List<Value>,
	private val mChannelValue: ChannelValue?
) : AliasComponent {
	override fun resolve(
		aliases: List<Alias>,
		parameters: ByteArray,
		channel: Byte
	): List<OutgoingMessage> {
		try {
			return aliases.first { it.mName.equals(mReferencedAliasName, ignoreCase = true) }
				.resolve(aliases, mArguments.map {
					it.resolve(parameters, mChannelValue?.mValue ?: channel)
				}.toByteArray(), mChannelValue?.mValue ?: channel)
		} catch (exception: NoSuchElementException) {
			throw ResolutionException(
				BeatPrompter.getResourceString(
					R.string.unknown_midi_directive,
					mReferencedAliasName
				)
			)
		}
	}
}
