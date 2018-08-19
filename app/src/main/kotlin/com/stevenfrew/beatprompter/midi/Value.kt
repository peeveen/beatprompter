package com.stevenfrew.beatprompter.midi

/**
 * A value in a MIDI component definition.
 * It can be a simple byte value, (CommandValue)
 * or a partial byte value with channel specifier, (ChannelCommandValue)
 * or a reference to an argument (ArgumentValue)
 */
abstract class Value {
    @Throws(ResolutionException::class)
    internal abstract fun resolve(arguments: ByteArray, channel: Byte): Byte

    internal abstract fun matches(otherValue: Value?): Boolean

    @Throws(ResolutionException::class)
    fun resolve(): Byte {
        return resolve(ByteArray(0), 0.toByte())
    }
}