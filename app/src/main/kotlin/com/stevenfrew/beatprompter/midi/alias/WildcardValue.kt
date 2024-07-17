package com.stevenfrew.beatprompter.midi.alias

/**
 * Represents a value that matches anything else.
 */
class WildcardValue : Value() {
	override fun resolve(arguments: ByteArray, channel: Byte): Byte = 0
	override fun matches(otherValue: Value?): Boolean = true
	override fun toString(): String = "*"
}