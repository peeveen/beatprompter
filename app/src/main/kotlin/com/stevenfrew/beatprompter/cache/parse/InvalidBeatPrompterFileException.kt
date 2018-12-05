package com.stevenfrew.beatprompter.cache.parse

import com.stevenfrew.beatprompter.BeatPrompterApplication

/**
 * Exception thrown when a file is not of the expected type, or is invalid in some way.
 */
internal class InvalidBeatPrompterFileException internal constructor(resourceId: Int,
                                                                     vararg args: Any)
    : Exception(BeatPrompterApplication.getResourceString(resourceId, *args))