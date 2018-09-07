package com.stevenfrew.beatprompter.cache.parse

/**
 * Exception thrown when a file is not of the expected type, or is invalid in some way.
 */
internal class InvalidBeatPrompterFileException(message: String) : Exception(message)