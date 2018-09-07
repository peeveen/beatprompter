package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor

/**
 * Base class for all file parsers.
 */
abstract class FileParser<TFileResult>(protected val mCachedCloudFileDescriptor:CachedCloudFileDescriptor) {
    protected val mErrors=mutableListOf<FileParseError>()

    @Throws(InvalidBeatPrompterFileException::class)
    abstract fun parse():TFileResult

    fun addError(error:FileParseError)
    {
        mErrors.add(error)
    }
}