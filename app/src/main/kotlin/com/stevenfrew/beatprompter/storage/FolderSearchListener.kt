package com.stevenfrew.beatprompter.storage

/**
 * Listener for the folder search task. Returns found items to the caller.
 */
interface FolderSearchListener : StorageListener {
	fun onCloudItemFound(item: ItemInfo)
	fun onFolderSearchError(t: Throwable)
	fun onFolderSearchComplete()
	fun onProgressMessageReceived(message: String)
}
