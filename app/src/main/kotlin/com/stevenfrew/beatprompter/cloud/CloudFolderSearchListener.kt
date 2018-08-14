package com.stevenfrew.beatprompter.cloud

interface CloudFolderSearchListener : CloudListener {
    fun onCloudItemFound(cloudItem: CloudItemInfo)
    fun onFolderSearchError(t: Throwable)
    fun onFolderSearchComplete()
    fun onProgressMessageReceived(message: String)
}
