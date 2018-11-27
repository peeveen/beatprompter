package com.stevenfrew.beatprompter.storage

interface FolderSearchListener : StorageListener {
    fun onCloudItemFound(item: ItemInfo)
    fun onFolderSearchError(t: Throwable)
    fun onFolderSearchComplete()
    fun onProgressMessageReceived(message: String)
}
