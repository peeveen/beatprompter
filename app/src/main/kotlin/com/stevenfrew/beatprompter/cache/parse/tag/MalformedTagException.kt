package com.stevenfrew.beatprompter.cache.parse.tag

import com.stevenfrew.beatprompter.BeatPrompter

/**
 * Exception thrown when a crap tag is found.
 */
class MalformedTagException : Exception {
    internal constructor(resourceId: Int, vararg args: Any)
            : this(BeatPrompter.getResourceString(resourceId, *args))

    internal constructor(message: String) : super(message)
    internal constructor(ex: Exception) : super(ex)
}