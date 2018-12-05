package com.stevenfrew.beatprompter.cache.parse.tag

import com.stevenfrew.beatprompter.BeatPrompterApplication

/**
 * Exception thrown when a crap tag is found.
 */
class MalformedTagException : Exception {
    internal constructor(resourceId: Int, vararg args: Any)
            : this(BeatPrompterApplication.getResourceString(resourceId, *args))

    internal constructor(message: String) : super(message)
    internal constructor(ex: Exception) : super(ex)
}