package com.stevenfrew.beatprompter.cache

class FileParseError(lineNumber: Int, private val mMessage: String?) {
    private var mLineNumber = -1

    val errorMessage: String
        get() = (if (mLineNumber != -1) "$mLineNumber: " else "") + mMessage

    constructor(tag: Tag?, message: String?) : this(tag?.mLineNumber ?: -1, message)

    init {
        mLineNumber = lineNumber
    }
}