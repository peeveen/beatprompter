package com.stevenfrew.beatprompter.storage

import android.app.ProgressDialog
import android.content.Context
import android.os.Handler
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.Cache
import com.stevenfrew.beatprompter.cache.CacheComparisonResult
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.cache.CachedFolder
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.util.CoroutineTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

/**
 * Task that downloads files from a storage system.
 */
class DownloadTask(
	private val context: Context,
	private val storage: Storage,
	private val handler: Handler,
	private val cloudPath: String,
	private val includeSubFolders: Boolean,
	filesToUpdate: List<CachedFile>?
) : CoroutineTask<Unit, String, Boolean> {
	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main
	private var progressDialog: ProgressDialog? = null
	private var errorOccurred = false
	private var filesToUpdate = filesToUpdate?.asSequence()
		?.map { ftu -> FileInfo(ftu.id, ftu.name, ftu.lastModified, ftu.subfolderIds) }
		?.toMutableList()
		?: mutableListOf()
	private var cloudItemsFound = mutableMapOf<String, ItemInfo>()

	private val isRefreshingSelectedFiles: Boolean
		get() = filesToUpdate.isNotEmpty()

	override fun onError(t: Throwable) {
		errorOccurred = true
		handler.obtainMessage(Events.CLOUD_SYNC_ERROR, t.message).sendToTarget()
	}

	private fun cancelWhenAuthenticationRequired() = cancel("Authentication required.")

	private fun closeProgressDialog() {
		if (progressDialog != null)
			progressDialog!!.dismiss()
	}

	override fun doInBackground(params: Unit, progressUpdater: suspend (String) -> Unit): Boolean {
		val itemDownloadListener = object : ItemDownloadListener {
			override fun onItemDownloaded(result: DownloadResult) {
				if (result is SuccessfulDownloadResult)
					Cache.cachedCloudItems.add(CachedFile.createCachedCloudFile(result))
				else
				// IMPLICIT if(result is FailedDownloadResult)
					Cache.cachedCloudItems.remove(result.fileInfo)
			}

			override suspend fun onProgressMessageReceived(message: String) = progressUpdater(message)

			override fun onDownloadError(t: Throwable) {
				onError(t)
				onDownloadComplete()
			}

			override fun onDownloadComplete() {
				if (!isRefreshingSelectedFiles)
					Cache.cachedCloudItems.removeNonExistent(
						cloudItemsFound.values.asSequence().map { c -> c.id }.toSet()
					)
				handler.obtainMessage(Events.CACHE_UPDATED, Cache.cachedCloudItems)
					.sendToTarget()
				closeProgressDialog()
			}

			override fun onAuthenticationRequired() = cancelWhenAuthenticationRequired()

			override fun shouldCancel(): Boolean = errorOccurred
		}
		val folderSearchListener = object : FolderSearchListener {
			override fun onCloudItemFound(item: ItemInfo) {
				val oldItem = cloudItemsFound[item.id]
				val itemToAdd =
					if (item is FileInfo && oldItem is FileInfo) {
						// If we've found this FILE already, then it exists in multiple folders.
						// Update the item with a new instance that reflects all sub-folders.
						val newSubfolderIDs = listOf(oldItem.subfolderIds, item.subfolderIds).flatten()
						FileInfo(oldItem.id, oldItem.name, oldItem.lastModified, newSubfolderIDs)
					} else
						oldItem ?: item
				cloudItemsFound[itemToAdd.id] = itemToAdd
			}

			override fun onFolderSearchError(t: Throwable, context: Context) {
				onError(t)
				closeProgressDialog()
			}

			override fun onFolderSearchComplete() {
				val itemsFound = cloudItemsFound.values.toList()
				itemsFound.filterIsInstance<FolderInfo>().forEach {
					val parentFolderID = it.parentFolder?.id
					val parentFolderIDs = if (parentFolderID == null) listOf() else listOf(parentFolderID)
					Cache.cachedCloudItems.add(CachedFolder(it.id, it.name, parentFolderIDs))
				}

				val downloadsAndUpdates =
					itemsFound
						.filterIsInstance<FileInfo>()
						.partition {
							Cache.cachedCloudItems.compareWithCacheVersion(it) == CacheComparisonResult.Newer
						}
				val itemsToDownload = downloadsAndUpdates.first
				val itemsToUpdate = downloadsAndUpdates.second

				itemsToUpdate.forEach {
					Cache.cachedCloudItems.updateLocations(it)
				}

				storage.downloadFiles(itemsToDownload, itemDownloadListener)
			}

			override suspend fun onProgressMessageReceived(message: String) = progressUpdater(message)
			override fun onAuthenticationRequired() = cancelWhenAuthenticationRequired()
			override fun shouldCancel(): Boolean = errorOccurred
		}
		if (isRefreshingSelectedFiles)
			updateSelectedFiles(itemDownloadListener)
		else
			updateEntireCache(folderSearchListener)
		return true
	}

	private fun updateEntireCache(listener: FolderSearchListener) =
		storage.readFolderContents(FolderInfo(cloudPath), listener, includeSubFolders)

	private fun updateSelectedFiles(listener: ItemDownloadListener) =
		storage.downloadFiles(filesToUpdate, listener)

	override fun onPreExecute() {
		progressDialog = ProgressDialog(context).apply {
			setTitle(BeatPrompter.appResources.getString(R.string.downloadingFiles))
			setMessage(
				BeatPrompter.appResources.getString(
					R.string.accessingCloudStorage,
					storage.cloudStorageName
				)
			)
			setCancelable(false)
			isIndeterminate = true
			show()
		}
	}

	override fun onProgressUpdate(progress: String) = progressDialog!!.setMessage(progress)

	override fun onPostExecute(result: Boolean) {
		// Don't care.
	}
}