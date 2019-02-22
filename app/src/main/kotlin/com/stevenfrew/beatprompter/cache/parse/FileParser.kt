package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.CachedFile

/**
 * Base class for all file parsers.
 */
abstract class FileParser<TFileResult>(protected val mCachedCloudFile: CachedFile) {
    protected val mErrors = mutableListOf<FileParseError>()

    abstract fun parse(): TFileResult

    fun addError(error: FileParseError) {
        mErrors.add(error)
    }
}