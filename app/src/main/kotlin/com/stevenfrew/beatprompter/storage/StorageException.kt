package com.stevenfrew.beatprompter.storage

import java.io.IOException

/**
 * A storage-specific exception.
 */
class StorageException(message: String) : IOException(message)
