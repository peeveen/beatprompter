package com.stevenfrew.beatprompter.storage

interface FolderSelectionListener : StorageListener {
    fun onFolderSelected(folderInfo: FolderInfo)
    fun onFolderSelectedError(t: Throwable)
}
