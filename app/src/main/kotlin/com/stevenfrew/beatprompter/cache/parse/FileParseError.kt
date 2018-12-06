package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

/**
 * A description of a parsing error that can be shown to the user.
 */
class FileParseError private constructor(val mLineNumber: Int, val mMessage: String) {

    constructor(lineNumber: Int, resourceId: Int, vararg args: Any) : this(lineNumber, BeatPrompter.getResourceString(resourceId, *args))
    constructor(tag: Tag, t: Throwable) : this(tag.mLineNumber, t.message ?: t.toString())
    constructor(lineNumber: Int, t: Throwable) : this(lineNumber, t.message ?: t.toString())
    constructor(tag: Tag, resourceId: Int, vararg args: Any) : this(tag.mLineNumber, resourceId, *args)
    constructor(resourceId: Int, vararg args: Any) : this(-1, resourceId, *args)

    override fun toString(): String {
        return (if (mLineNumber != -1) "$mLineNumber: " else "") + mMessage
    }

    override fun hashCode(): Int {
        return 31 * mLineNumber.hashCode() + mMessage.hashCode()
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