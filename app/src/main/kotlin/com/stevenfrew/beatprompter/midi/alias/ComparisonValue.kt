package com.stevenfrew.beatprompter.midi.alias

internal class ComparisonValue(private val type: ComparisonType, value: Byte) :
	CommandValue(value) {
	override fun matches(otherValue: Value?): Boolean {
		if (otherValue is WildcardValue)
			return true
		if (otherValue == null)
			return false
		return otherValue.resolve().let {
			when (type) {
				ComparisonType.LessThan -> it < value
				ComparisonType.LessThanOrEqualTo -> it <= value
				ComparisonType.GreaterThan -> it > value
				ComparisonType.GreaterThanOrEqualTo -> it >= value
			}
		}
	}
}