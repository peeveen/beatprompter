package com.stevenfrew.beatprompter.midi.alias

/**
 * Represents "no value", used for matching against nothing.
 */
class NoValue : Value() {
	override fun resolve(arguments: ByteArray, channel: Byte): Byte = 0
	override fun matches(otherValue: Value?): Boolean = false
	override fun toString(): String = ""
}