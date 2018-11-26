package com.stevenfrew.beatprompter.cache.parse.tag

/**
 * Exception thrown when a crap tag is found.
 */
class MalformedTagException : Exception {
    internal constructor(message: String) : super(message)
    internal constructor(ex: Exception) : super(ex)
}