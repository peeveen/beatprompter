package com.stevenfrew.beatprompter.midi.alias

data class AliasSet(
	val name: String,
	val aliases: List<Alias>,
	val useByDefault: Boolean = true
)