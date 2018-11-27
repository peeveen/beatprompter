package com.stevenfrew.beatprompter.storage

/**
 * Listener for the operation that retrieves the root path of a storage system.
 * Returns the root path to the user.
 */
interface RootPathListener : StorageListener {
    fun onRootPathFound(rootPath: FolderInfo)
    fun onRootPathError(t: Throwable)
}