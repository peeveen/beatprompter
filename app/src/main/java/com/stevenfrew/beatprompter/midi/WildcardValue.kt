package com.stevenfrew.beatprompter.midi

/**
 * Represents a value that matches anything else.
 */
class WildcardValue : Value() {

    internal override fun resolve(arguments: ByteArray, channel: Byte): Byte {
        return 0
    }

    internal override fun matches(otherValue: Value?): Boolean {
        return true
    }

    override fun toString(): String {
        return "*"
    }
}