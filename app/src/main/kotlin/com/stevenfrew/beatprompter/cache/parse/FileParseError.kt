package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.parse.tag.Tag

class FileParseError(lineNumber: Int, private val mMessage: String) {
    private val mLineNumber = lineNumber

    constructor(tag: Tag?, message: String) : this(tag?.mLineNumber ?: -1, message)

    override fun toString(): String {
        return (if (mLineNumber != -1) "$mLineNumber: " else "") + mMessage
    }

    override fun hashCode(): Int {
        var result = mLineNumber.hashCode()
        result = 31 * result + mMessage.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileParseError

        if (mMessage != other.mMessage) return false
        if (mLineNumber != other.mLineNumber) return false

        return true
    }
}