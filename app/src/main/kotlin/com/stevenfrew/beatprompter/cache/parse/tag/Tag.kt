package com.stevenfrew.beatprompter.cache.parse.tag

/**
 * Base class for all tags. Contains the base member variables.
 */
abstract class Tag protected constructor(
	val name: String,
	internal val lineNumber: Int,
	val position: Int
)
