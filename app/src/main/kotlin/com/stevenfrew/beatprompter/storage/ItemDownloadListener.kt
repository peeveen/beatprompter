package com.stevenfrew.beatprompter.storage

interface ItemDownloadListener : StorageListener {
    fun onItemDownloaded(result: DownloadResult)
    fun onProgressMessageReceived(message: String)
    fun onDownloadError(t: Throwable)
    fun onDownloadComplete()
}
