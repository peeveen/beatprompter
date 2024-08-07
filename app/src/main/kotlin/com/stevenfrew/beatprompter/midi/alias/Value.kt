package com.stevenfrew.beatprompter.midi.alias

/**
 * A value in a MIDI component definition.
 * It can be a simple byte value, (CommandValue)
 * or a partial byte value with channel specifier, (ChannelCommandValue)
 * or a reference to an argument (ArgumentValue)
 */
abstract class Value {
	internal abstract fun resolve(arguments: ByteArray, channel: Byte): Byte

	internal abstract fun matches(otherValue: Value?): Boolean

	fun resolve(): Byte = resolve(ByteArray(0), 0)

	companion object {
		internal const val ZERO_BYTE: Byte = 0
	}
}