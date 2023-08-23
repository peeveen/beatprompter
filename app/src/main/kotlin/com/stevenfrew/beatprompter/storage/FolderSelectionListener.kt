package com.stevenfrew.beatprompter.storage

import android.content.Context

/**
 * Listener for the task that allows the user to choose a storage folder.
 * Returns the selected folder to the user.
 */
interface FolderSelectionListener : StorageListener {
	fun onFolderSelected(folderInfo: FolderInfo)
	fun onFolderSelectedError(t: Throwable, context: Context)
	fun onFolderSelectionComplete()
}
