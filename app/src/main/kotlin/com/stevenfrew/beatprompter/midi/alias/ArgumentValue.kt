package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R

internal class ArgumentValue(private val mArgumentIndex: Int) : Value() {

    init {
        if (mArgumentIndex < 0)
            throw ValueException(BeatPrompter.getResourceString(R.string.not_a_valid_argument_index))
    }

    override fun resolve(arguments: ByteArray, channel: Byte): Byte {
        if (mArgumentIndex >= arguments.size)
            throw ResolutionException(BeatPrompter.getResourceString(R.string.not_enough_parameters_supplied))
        return arguments[mArgumentIndex]
    }

    override fun matches(otherValue: Value?): Boolean {
        return if (otherValue is ArgumentValue) otherValue.mArgumentIndex == mArgumentIndex else otherValue is WildcardValue
    }
}
