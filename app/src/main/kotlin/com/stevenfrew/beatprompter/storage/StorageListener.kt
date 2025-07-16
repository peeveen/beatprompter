package com.stevenfrew.beatprompter.storage

/**
 * Listener for general storage features.
 */
interface StorageListener {
	fun shouldCancel(): Boolean
}