package com.stevenfrew.beatprompter.cloud

interface CloudItemDownloadListener : CloudListener {
    fun onItemDownloaded(result: CloudDownloadResult)
    fun onProgressMessageReceived(message: String)
    fun onDownloadError(t: Throwable)
    fun onDownloadComplete()
}
