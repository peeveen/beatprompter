package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.parse.tag.Tag

class FileParseError(lineNumber: Int, private val mMessage: String?) {
    private val mLineNumber = lineNumber

    constructor(tag: Tag?, message: String?) : this(tag?.mLineNumber ?: -1, message)

    val errorMessage: String
        get() = (if (mLineNumber != -1) "$mLineNumber: " else "") + mMessage

}