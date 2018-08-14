package com.stevenfrew.beatprompter.cloud

interface CloudRootPathListener : CloudListener {
    fun onRootPathFound(rootPath: CloudFolderInfo)
    fun onRootPathError(t: Throwable)
}