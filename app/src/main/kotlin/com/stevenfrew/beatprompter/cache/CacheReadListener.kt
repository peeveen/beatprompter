package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.util.ProgressReportingListener

/**
 * Listener for the database read task.
 */
interface CacheReadListener : ProgressReportingListener<String> {
	fun onItemRead(cachedFile: CachedItem)
	fun onCacheReadError(t: Throwable)
	fun onCacheReadComplete()
}
