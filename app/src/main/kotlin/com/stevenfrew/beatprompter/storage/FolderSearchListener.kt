package com.stevenfrew.beatprompter.storage

import android.content.Context

/**
 * Listener for the folder search task. Returns found items to the caller.
 */
interface FolderSearchListener : StorageListener {
	fun onCloudItemFound(item: ItemInfo)
	fun onFolderSearchError(t: Throwable, context: Context)
	fun onFolderSearchComplete()
	fun onProgressMessageReceived(message: String)
}
