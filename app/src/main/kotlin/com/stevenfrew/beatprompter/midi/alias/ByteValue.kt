package com.stevenfrew.beatprompter.midi.alias

abstract class ByteValue internal constructor(internal val value: Byte) : Value() {

	override fun resolve(arguments: ByteArray, channel: Byte): Byte = value

	override fun matches(otherValue: Value?): Boolean =
		if (otherValue is ByteValue) otherValue.value == value else otherValue is WildcardValue

	override fun toString(): String = "$value"
}

