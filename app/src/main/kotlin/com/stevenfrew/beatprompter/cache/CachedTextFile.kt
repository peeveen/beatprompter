package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.FileParseError

/**
 * Base class for a cached text file.
 */
abstract class CachedTextFile internal constructor(cachedCloudFileDescriptor: CachedFileDescriptor,
                                                   val mErrors: List<FileParseError>)
    : CachedFile(cachedCloudFileDescriptor)