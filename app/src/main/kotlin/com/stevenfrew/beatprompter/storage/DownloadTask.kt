package com.stevenfrew.beatprompter.storage

import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Handler
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ui.SongListActivity
import com.stevenfrew.beatprompter.cache.CachedFile

class DownloadTask(private val mStorage: Storage, private val mHandler: Handler, private val mCloudPath: String, private val mIncludeSubFolders: Boolean, filesToUpdate: List<CachedFile>?) : AsyncTask<String, String, Boolean>(), FolderSearchListener, ItemDownloadListener {
    private var mProgressDialog: ProgressDialog? = null
    private var mErrorOccurred = false
    private var mFilesToUpdate = filesToUpdate?.asSequence()?.map { ftu -> FileInfo(ftu.mID, ftu.mName, ftu.mLastModified, ftu.mSubfolder) }?.toMutableList()
            ?: mutableListOf()
    private val mCloudFilesFound = mutableListOf<FileInfo>()
    private val mCloudFilesToDownload = mutableListOf<FileInfo>()

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
            setTitle(BeatPrompterApplication.getResourceString(R.string.downloadingFiles))
            setMessage(BeatPrompterApplication.getResourceString(R.string.accessingCloudStorage, mStorage.cloudStorageName))
            setCancelable(false)
            isIndeterminate = true
            show()
        }
    }

    override fun onCloudItemFound(item: ItemInfo) {
        // We're only interested in downloading files.
        if (item is FileInfo) {
            mCloudFilesFound.add(item)
            if (!SongListActivity.mCachedCloudFiles.hasLatestVersionOf(item))
                mCloudFilesToDownload.add(item)
        }
    }

    override fun onFolderSearchError(t: Throwable) {
        mErrorOccurred = true
        mHandler.obtainMessage(EventHandler.CLOUD_SYNC_ERROR, t.message).sendToTarget()
        if (mProgressDialog != null)
            mProgressDialog!!.dismiss()
    }

    override fun onFolderSearchComplete() {
        mStorage.downloadFiles(mCloudFilesToDownload, this)
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
        mHandler.obtainMessage(EventHandler.CLOUD_SYNC_ERROR, t.message).sendToTarget()
        onDownloadComplete()
    }

    override fun onDownloadComplete() {
        if (!isRefreshingSelectedFiles)
            SongListActivity.mCachedCloudFiles.removeNonExistent(mCloudFilesFound.asSequence().map { c -> c.mID }.toSet())
        mHandler.obtainMessage(EventHandler.CACHE_UPDATED, SongListActivity.mCachedCloudFiles).sendToTarget()
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