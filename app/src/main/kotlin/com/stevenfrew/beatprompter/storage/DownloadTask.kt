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
	private val mContext: Context,
	private val mStorage: Storage,
	private val mHandler: Handler,
	private val mCloudPath: String,
	private val mIncludeSubFolders: Boolean,
	filesToUpdate: List<CachedFile>?
) : CoroutineTask<Unit, String, Boolean> {
	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Main
	private var mProgressDialog: ProgressDialog? = null
	private var mErrorOccurred = false
	private var mFilesToUpdate = filesToUpdate?.asSequence()
		?.map { ftu -> FileInfo(ftu.mID, ftu.mName, ftu.mLastModified, ftu.mSubfolderIDs) }
		?.toMutableList()
		?: mutableListOf()
	private var mCloudItemsFound = mutableMapOf<String, ItemInfo>()

	private val isRefreshingSelectedFiles: Boolean
		get() = mFilesToUpdate.isNotEmpty()

	override fun onError(t: Throwable) {
		mErrorOccurred = true
		mHandler.obtainMessage(Events.CLOUD_SYNC_ERROR, t.message).sendToTarget()
	}

	private fun cancelWhenAuthenticationRequired() = cancel("Authentication required.")

	private fun closeProgressDialog() {
		if (mProgressDialog != null)
			mProgressDialog!!.dismiss()
	}

	override fun doInBackground(params: Unit, progressUpdater: suspend (String) -> Unit): Boolean {
		val itemDownloadListener = object : ItemDownloadListener {
			override fun onItemDownloaded(result: DownloadResult) {
				if (result is SuccessfulDownloadResult)
					Cache.mCachedCloudItems.add(CachedFile.createCachedCloudFile(result))
				else
				// IMPLICIT if(result is FailedDownloadResult)
					Cache.mCachedCloudItems.remove(result.mFileInfo)
			}

			override suspend fun onProgressMessageReceived(message: String) = progressUpdater(message)

			override fun onDownloadError(t: Throwable) {
				onError(t)
				onDownloadComplete()
			}

			override fun onDownloadComplete() {
				if (!isRefreshingSelectedFiles)
					Cache.mCachedCloudItems.removeNonExistent(
						mCloudItemsFound.values.asSequence().map { c -> c.mID }.toSet()
					)
				mHandler.obtainMessage(Events.CACHE_UPDATED, Cache.mCachedCloudItems)
					.sendToTarget()
				closeProgressDialog()
			}

			override fun onAuthenticationRequired() = cancelWhenAuthenticationRequired()

			override fun shouldCancel(): Boolean = mErrorOccurred
		}
		val folderSearchListener = object : FolderSearchListener {
			override fun onCloudItemFound(item: ItemInfo) {
				val oldItem = mCloudItemsFound[item.mID]
				val itemToAdd =
					if (item is FileInfo && oldItem is FileInfo) {
						// If we've found this FILE already, then it exists in multiple folders.
						// Update the item with a new instance that reflects all sub-folders.
						val newSubfolderIDs = listOf(oldItem.mSubfolderIDs, item.mSubfolderIDs).flatten()
						FileInfo(oldItem.mID, oldItem.mName, oldItem.mLastModified, newSubfolderIDs)
					} else
						oldItem ?: item
				mCloudItemsFound[itemToAdd.mID] = itemToAdd
			}

			override fun onFolderSearchError(t: Throwable, context: Context) {
				onError(t)
				closeProgressDialog()
			}

			override fun onFolderSearchComplete() {
				val itemsFound = mCloudItemsFound.values.toList()
				itemsFound.filterIsInstance<FolderInfo>().forEach {
					val parentFolderID = it.mParentFolder?.mID
					val parentFolderIDs = if (parentFolderID == null) listOf() else listOf(parentFolderID)
					Cache.mCachedCloudItems.add(CachedFolder(it.mID, it.mName, parentFolderIDs))
				}

				val downloadsAndUpdates =
					itemsFound
						.filterIsInstance<FileInfo>()
						.partition {
							Cache.mCachedCloudItems.compareWithCacheVersion(it) == CacheComparisonResult.Newer
						}
				val itemsToDownload = downloadsAndUpdates.first
				val itemsToUpdate = downloadsAndUpdates.second

				itemsToUpdate.forEach {
					Cache.mCachedCloudItems.updateLocations(it)
				}

				mStorage.downloadFiles(itemsToDownload, itemDownloadListener)
			}

			override suspend fun onProgressMessageReceived(message: String) = progressUpdater(message)
			override fun onAuthenticationRequired() = cancelWhenAuthenticationRequired()
			override fun shouldCancel(): Boolean = mErrorOccurred
		}
		if (isRefreshingSelectedFiles)
			updateSelectedFiles(itemDownloadListener)
		else
			updateEntireCache(folderSearchListener)
		return true
	}

	private fun updateEntireCache(listener: FolderSearchListener) =
		mStorage.readFolderContents(FolderInfo(mCloudPath), listener, mIncludeSubFolders)

	private fun updateSelectedFiles(listener: ItemDownloadListener) =
		mStorage.downloadFiles(mFilesToUpdate, listener)

	override fun onPreExecute() {
		mProgressDialog = ProgressDialog(mContext).apply {
			setTitle(BeatPrompter.appResources.getString(R.string.downloadingFiles))
			setMessage(
				BeatPrompter.appResources.getString(
					R.string.accessingCloudStorage,
					mStorage.cloudStorageName
				)
			)
			setCancelable(false)
			isIndeterminate = true
			show()
		}
	}

	override fun onProgressUpdate(progress: String) = mProgressDialog!!.setMessage(progress)

	override fun onPostExecute(result: Boolean) {
		// Don't care.
	}
}