package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R

internal class ArgumentValue(val argumentIndex: Int) : Value() {
	init {
		if (argumentIndex < 0)
			throw ValueException(BeatPrompter.appResources.getString(R.string.not_a_valid_argument_index))
	}

	override fun resolve(arguments: ByteArray, channel: Byte): Byte {
		if (argumentIndex >= arguments.size)
			throw ResolutionException(BeatPrompter.appResources.getString(R.string.not_enough_parameters_supplied))
		return arguments[argumentIndex]
	}

	override fun matches(otherValue: Value?): Boolean =
		if (otherValue is ArgumentValue) otherValue.argumentIndex == argumentIndex else otherValue is WildcardValue
}
