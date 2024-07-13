package com.stevenfrew.beatprompter.database

import com.stevenfrew.beatprompter.cache.CachedItem
import com.stevenfrew.beatprompter.util.ProgressReportingListener

/**
 * Listener for the database read task.
 */
interface DatabaseReadListener : ProgressReportingListener<String> {
	fun onItemRead(cachedFile:CachedItem)
	fun onDatabaseReadError(t: Throwable)
	fun onDatabaseReadComplete()
}
