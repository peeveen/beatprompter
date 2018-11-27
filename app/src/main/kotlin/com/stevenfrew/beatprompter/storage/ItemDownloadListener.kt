package com.stevenfrew.beatprompter.storage

/**
 * Listener for the task that downloads files from the storage.
 * Tells the user what has been downloaded.
 */
interface ItemDownloadListener : StorageListener {
    fun onItemDownloaded(result: DownloadResult)
    fun onProgressMessageReceived(message: String)
    fun onDownloadError(t: Throwable)
    fun onDownloadComplete()
}
