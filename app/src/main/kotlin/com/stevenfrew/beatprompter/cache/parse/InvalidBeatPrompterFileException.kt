package com.stevenfrew.beatprompter.cache.parse

import java.io.IOException

internal class InvalidBeatPrompterFileException : IOException {
    constructor(message: String) : super(message)
    constructor(message: String, inner: Exception) : super(message, inner)
}