package com.stevenfrew.beatprompter.storage

import com.stevenfrew.beatprompter.util.ProgressReportingListener

/**
 * Listener for the task that downloads files from the storage.
 * Tells the user what has been downloaded.
 */
interface ItemDownloadListener : StorageListener, ProgressReportingListener<String> {
	fun onItemDownloaded(result: DownloadResult)
	fun onDownloadError(t: Throwable)
	fun onDownloadComplete()
}
