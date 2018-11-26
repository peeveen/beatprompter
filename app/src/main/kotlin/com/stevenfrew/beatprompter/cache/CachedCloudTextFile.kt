package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.FileParseError

/**
 * Base class for a cached text file.
 */
abstract class CachedCloudTextFile internal constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor, val mErrors: List<FileParseError>) : CachedCloudFile(cachedCloudFileDescriptor)