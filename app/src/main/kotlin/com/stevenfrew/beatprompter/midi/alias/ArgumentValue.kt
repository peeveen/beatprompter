package com.stevenfrew.beatprompter.midi.alias

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R

internal class ArgumentValue(private val mArgumentIndex: Int) : Value() {

    init {
        if (mArgumentIndex < 0)
            throw ValueException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_argument_index))
    }

    @Throws(ResolutionException::class)
    override fun resolve(arguments: ByteArray, channel: Byte): Byte {
        if (mArgumentIndex >= arguments.size)
            throw ResolutionException(BeatPrompterApplication.getResourceString(R.string.not_enough_parameters_supplied))
        return arguments[mArgumentIndex]
    }

    override fun matches(otherValue: Value?): Boolean {
        return if (otherValue is ArgumentValue) otherValue.mArgumentIndex == mArgumentIndex else otherValue is WildcardValue
    }
}
