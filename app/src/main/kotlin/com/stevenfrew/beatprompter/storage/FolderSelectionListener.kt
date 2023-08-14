package com.stevenfrew.beatprompter.storage

/**
 * Listener for the task that allows the user to choose a storage folder.
 * Returns the selected folder to the user.
 */
interface FolderSelectionListener : StorageListener {
	fun onFolderSelected(folderInfo: FolderInfo)
	fun onFolderSelectedError(t: Throwable)
}
