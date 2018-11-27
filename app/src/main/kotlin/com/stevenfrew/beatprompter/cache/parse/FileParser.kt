package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.CachedFileDescriptor

/**
 * Base class for all file parsers.
 */
abstract class FileParser<TFileResult> constructor(protected val mCachedCloudFileDescriptor: CachedFileDescriptor) {
    protected val mErrors = mutableListOf<FileParseError>()

    @Throws(InvalidBeatPrompterFileException::class)
    abstract fun parse(): TFileResult

    fun addError(error: FileParseError) {
        mErrors.add(error)
    }
}