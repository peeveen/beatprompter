package com.stevenfrew.beatprompter.cloud

interface CloudFolderSelectionListener : CloudListener {
    fun onFolderSelected(folderInfo: CloudFolderInfo)
    fun onFolderSelectedError(t: Throwable)
}
