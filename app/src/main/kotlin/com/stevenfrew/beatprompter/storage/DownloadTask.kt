package com.stevenfrew.beatprompter.storage

import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Handler
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Events
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CacheComparisonResult
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.cache.CachedFolder
import com.stevenfrew.beatprompter.ui.SongListActivity

/**
 * Task that downloads files from a storage system.
 */
class DownloadTask(private val mStorage: Storage,
                   private val mHandler: Handler,
                   private val mCloudPath: String,
                   private val mIncludeSubFolders: Boolean,
                   filesToUpdate: List<CachedFile>?)
    : AsyncTask<String, String, Boolean>(), FolderSearchListener, ItemDownloadListener {
    private var mProgressDialog: ProgressDialog? = null
    private var mErrorOccurred = false
    private var mFilesToUpdate = filesToUpdate?.asSequence()?.map { ftu -> FileInfo(ftu.mID, ftu.mName, ftu.mLastModified, ftu.mSubfolderIDs) }?.toMutableList()
            ?: mutableListOf()
    private var mCloudItemsFound = mutableMapOf<String, ItemInfo>()

    private val isRefreshingSelectedFiles: Boolean
        get() = mFilesToUpdate.isNotEmpty()

    override fun doInBackground(vararg paramParams: String): Boolean? {
        if (isRefreshingSelectedFiles)
            updateSelectedFiles()
        else
            updateEntireCache()
        return true
    }

    private fun updateEntireCache() {
        mStorage.readFolderContents(FolderInfo(mCloudPath), this, mIncludeSubFolders)
    }

    private fun updateSelectedFiles() {
        mStorage.downloadFiles(mFilesToUpdate, this)
    }

    override fun onProgressUpdate(vararg values: String) {
        super.onProgressUpdate(*values)
        mProgressDialog!!.setMessage(values[0])
    }

    override fun onPreExecute() {
        super.onPreExecute()
        mProgressDialog = ProgressDialog(SongListActivity.mSongListInstance).apply {
            setTitle(BeatPrompter.getResourceString(R.string.downloadingFiles))
            setMessage(BeatPrompter.getResourceString(R.string.accessingCloudStorage, mStorage.cloudStorageName))
            setCancelable(false)
            isIndeterminate = true
            show()
        }
    }

    override fun onCloudItemFound(item: ItemInfo) {
        val oldItem = mCloudItemsFound[item.mID]
        val itemToAdd =
                if (item is FileInfo && oldItem is FileInfo) {
                    // If we've found this FILE already, then it exists in multiple folders.
                    // Update the item with a new instance that reflects all subfolders.
                    val newSubfolderIDs = listOf(oldItem.mSubfolderIDs, item.mSubfolderIDs).flatten()
                    FileInfo(oldItem.mID, oldItem.mName, oldItem.mLastModified, newSubfolderIDs)
                } else
                    oldItem ?: item
        mCloudItemsFound[itemToAdd.mID] = itemToAdd

    }

    override fun onFolderSearchError(t: Throwable) {
        mErrorOccurred = true
        mHandler.obtainMessage(Events.CLOUD_SYNC_ERROR, t.message).sendToTarget()
        if (mProgressDialog != null)
            mProgressDialog!!.dismiss()
    }

    override fun onFolderSearchComplete() {
        val itemsFound = mCloudItemsFound.values.toList()
        itemsFound.filterIsInstance<FolderInfo>().forEach {
            val parentFolderID = it.mParentFolder?.mID
            val parentFolderIDs = if (parentFolderID == null) listOf() else listOf(parentFolderID)
            SongListActivity.mCachedCloudItems.add(CachedFolder(it.mID, it.mName, parentFolderIDs))
        }

        val downloadsAndUpdates =
                itemsFound
                        .filterIsInstance<FileInfo>()
                        .partition {
                            SongListActivity.mCachedCloudItems.compareWithCacheVersion(it) == CacheComparisonResult.Newer
                        }
        val itemsToDownload = downloadsAndUpdates.first
        val itemsToUpdate = downloadsAndUpdates.second

        itemsToUpdate.forEach {
            SongListActivity.mCachedCloudItems.updateLocations(it)
        }

        mStorage.downloadFiles(itemsToDownload, this)
    }

    override fun onItemDownloaded(result: DownloadResult) {
        if (result is SuccessfulDownloadResult)
            SongListActivity.mCachedCloudItems.add(CachedFile.createCachedCloudFile(result))
        else
        // IMPLICIT if(result is FailedDownloadResult)
            SongListActivity.mCachedCloudItems.remove(result.mFileInfo)
    }

    override fun onProgressMessageReceived(message: String) {
        publishProgress(message)
    }

    override fun onDownloadError(t: Throwable) {
        mErrorOccurred = true
        mHandler.obtainMessage(Events.CLOUD_SYNC_ERROR, t.message).sendToTarget()
        onDownloadComplete()
    }

    override fun onDownloadComplete() {
        if (!isRefreshingSelectedFiles)
            SongListActivity.mCachedCloudItems.removeNonExistent(mCloudItemsFound.values.asSequence().map { c -> c.mID }.toSet())
        mHandler.obtainMessage(Events.CACHE_UPDATED, SongListActivity.mCachedCloudItems).sendToTarget()
        if (mProgressDialog != null)
            mProgressDialog!!.dismiss()
    }

    override fun onAuthenticationRequired() {
        this.cancel(true)
    }

    override fun shouldCancel(): Boolean {
        return mErrorOccurred || isCancelled
    }
}