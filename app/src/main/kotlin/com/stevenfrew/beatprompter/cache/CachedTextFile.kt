package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.FileParseError

/**
 * Base class for a cached text file.
 */
abstract class CachedTextFile internal constructor(cachedFile: CachedFile,
                                                   val mErrors: List<FileParseError>)
    : CachedFile(cachedFile)