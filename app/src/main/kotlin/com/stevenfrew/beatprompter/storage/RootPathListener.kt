package com.stevenfrew.beatprompter.storage

interface RootPathListener : StorageListener {
    fun onRootPathFound(rootPath: FolderInfo)
    fun onRootPathError(t: Throwable)
}