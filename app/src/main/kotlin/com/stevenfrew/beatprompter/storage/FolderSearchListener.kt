package com.stevenfrew.beatprompter.storage

import android.content.Context
import com.stevenfrew.beatprompter.util.ProgressReportingListener

/**
 * Listener for the folder search task. Returns found items to the caller.
 */
interface FolderSearchListener : StorageListener, ProgressReportingListener<String> {
	fun onCloudItemFound(item: ItemInfo)
	fun onFolderSearchError(t: Throwable, context: Context)
	fun onFolderSearchComplete()
}
