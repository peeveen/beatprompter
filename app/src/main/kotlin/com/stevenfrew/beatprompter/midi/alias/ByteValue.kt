package com.stevenfrew.beatprompter.midi.alias

abstract class ByteValue internal constructor(internal val mValue: Byte) : Value() {

	override fun resolve(arguments: ByteArray, channel: Byte): Byte {
		return mValue
	}

	override fun matches(otherValue: Value?): Boolean {
		return if (otherValue is ByteValue) otherValue.mValue == mValue else otherValue is WildcardValue
	}

	override fun toString(): String {
		return "" + mValue
	}
}

