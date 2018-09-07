package com.stevenfrew.beatprompter.cache.parse

import java.io.IOException

/**
 * Special exception for song parsing errors.
 */
internal class SongParserException(message: String) : IOException(message)