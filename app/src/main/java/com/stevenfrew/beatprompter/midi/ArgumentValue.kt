package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R

internal class ArgumentValue(private val mArgumentIndex: Int) : Value() {

    @Throws(ResolutionException::class)
    internal override fun resolve(arguments: ByteArray, channel: Byte): Byte {
        if (mArgumentIndex >= arguments.size)
            throw ResolutionException(BeatPrompterApplication.getResourceString(R.string.not_enough_parameters_supplied))
        return arguments[mArgumentIndex]
    }

    internal override fun matches(otherValue: Value?): Boolean {
        return if (otherValue is ArgumentValue) otherValue.mArgumentIndex == mArgumentIndex else otherValue is WildcardValue
    }
}
