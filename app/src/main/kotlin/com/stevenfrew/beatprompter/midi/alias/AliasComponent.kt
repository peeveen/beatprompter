package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage

interface AliasComponent {
	val parameterCount: Int
	fun resolve(
		sourceAliasSet: AliasSet,
		aliases: List<AliasSet>,
		parameters: ByteArray,
		channel: Byte
	): Pair<List<MidiMessage>, Set<AliasSet>>
}