package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompter

/**
 * Exception thrown when a file is not of the expected type, or is invalid in some way.
 */
internal class InvalidBeatPrompterFileException internal constructor(
	resourceId: Int,
	vararg args: Any
) : Exception(BeatPrompter.getResourceString(resourceId, *args))