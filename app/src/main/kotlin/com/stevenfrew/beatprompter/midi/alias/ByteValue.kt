package com.stevenfrew.beatprompter.midi.alias

abstract class ByteValue internal constructor(internal val value: Byte) : Value() {

	override fun resolve(arguments: ByteArray, channel: Byte): Byte = value

	override fun matches(otherValue: Value?): Boolean =
		when (otherValue) {
			is ComparisonValue -> otherValue.matches(this)
			is ByteValue -> otherValue.value == value
			is WildcardValue -> true
			else -> false
		}

	override fun toString(): String = "$value"
}

