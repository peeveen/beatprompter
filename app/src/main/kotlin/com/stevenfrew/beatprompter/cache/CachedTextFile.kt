package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.ContentParsingError

/**
 * Base class for a cached text file.
 */
abstract class CachedTextFile internal constructor(
	cachedFile: CachedFile,
	val errors: List<ContentParsingError>
) : CachedFile(cachedFile)