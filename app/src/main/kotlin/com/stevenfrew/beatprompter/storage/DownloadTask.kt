package com.stevenfrew.beatprompter.storage

import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Handler
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Events
import com.stevenfrew.beatprompter.R
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
    private var mCloudItemsFound = mutableListOf<ItemInfo>()

    private val isRefreshingSelectedFiles: Boolean
        get() = !mFilesToUpdate.isEmpty()

    override fun doInBackground(vararg paramParams: String): Boolean? {
        if (isRefreshingSelectedFiles)
            updateSelectedFiles()
        else
            updateEntireCache()
        return true
    }

    private fun updateEntireCache() {
        mStorage.readFolderContents(FolderInfo(mCloudPath), this, mIncludeSubFolders, false)
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
        mCloudItemsFound.add(item)
    }

    override fun onFolderSearchError(t: Throwable) {
        mErrorOccurred = true
        mHandler.obtainMessage(Events.CLOUD_SYNC_ERROR, t.message).sendToTarget()
        if (mProgressDialog != null)
            mProgressDialog!!.dismiss()
    }

    override fun onFolderSearchComplete() {
        val itemsWithSubfolderIDsPopulated =
                mCloudItemsFound
                        .filterIsInstance<FileInfo>()
                        .groupBy { it.mID }
                        .map {
                            val allSubfolderIDs = it.value.mapNotNull { item -> item.mSubfolderIDs.firstOrNull() }
                            FileInfo(it.key, it.value.first().mName, it.value.first().mLastModified, allSubfolderIDs)
                        }

        mCloudItemsFound.filterIsInstance<FolderInfo>().forEach {
            val parentFolderID = it.mParentFolder?.mID
            val parentFolderIDs = if (parentFolderID == null) listOf() else listOf(parentFolderID)
            SongListActivity.mCachedCloudFiles.add(CachedFolder(it.mID, it.mName, it.mFilterOnly, parentFolderIDs))
        }

        // TODO: Don't download if it's just the subfolders or filter_only status that have changed.
        mCloudItemsFound = itemsWithSubfolderIDsPopulated.toMutableList()
        val cloudFilesToDownload =
                mCloudItemsFound
                        .filterIsInstance<FileInfo>()
                        .filter {
                            !SongListActivity.mCachedCloudFiles.hasLatestVersionOf(it)
                        }
                        .toMutableList()

        mStorage.downloadFiles(cloudFilesToDownload, this)
    }

    override fun onItemDownloaded(result: DownloadResult) {
        if (result is SuccessfulDownloadResult)
            SongListActivity.mCachedCloudFiles.add(CachedFile.createCachedCloudFile(result))
        else
        // IMPLICIT if(result is FailedDownloadResult)
            SongListActivity.mCachedCloudFiles.remove(result.mFileInfo)
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
            SongListActivity.mCachedCloudFiles.removeNonExistent(mCloudItemsFound.asSequence().map { c -> c.mID }.toSet())
        mHandler.obtainMessage(Events.CACHE_UPDATED, SongListActivity.mCachedCloudFiles).sendToTarget()
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