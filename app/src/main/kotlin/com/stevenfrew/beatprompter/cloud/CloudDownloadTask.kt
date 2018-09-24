package com.stevenfrew.beatprompter.cloud

import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Handler
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ui.SongListActivity
import com.stevenfrew.beatprompter.cache.CachedCloudFile

class CloudDownloadTask(private val mCloudStorage: CloudStorage, private val mHandler: Handler, private val mCloudPath: String, private val mIncludeSubFolders: Boolean, filesToUpdate: List<CachedCloudFile>?) : AsyncTask<String, String, Boolean>(), CloudFolderSearchListener, CloudItemDownloadListener {
    private var mProgressDialog: ProgressDialog? = null
    private var mErrorOccurred = false
    private var mFilesToUpdate= filesToUpdate?.asSequence()?.map { ftu -> CloudFileInfo(ftu.mID, ftu.mName, ftu.mLastModified, ftu.mSubfolder) }?.toMutableList() ?: mutableListOf()
    private val mCloudFilesFound = mutableListOf<CloudFileInfo>()
    private val mCloudFilesToDownload = mutableListOf<CloudFileInfo>()

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
        mCloudStorage.readFolderContents(CloudFolderInfo(mCloudPath), this, mIncludeSubFolders, false)
    }

    private fun updateSelectedFiles() {
        mCloudStorage.downloadFiles(mFilesToUpdate, this)
    }

    override fun onProgressUpdate(vararg values: String) {
        super.onProgressUpdate(*values)
        mProgressDialog!!.setMessage(values[0])
    }

    override fun onPreExecute() {
        super.onPreExecute()
        mProgressDialog = ProgressDialog(SongListActivity.mSongListInstance).apply {
            setTitle(BeatPrompterApplication.getResourceString(R.string.downloadingFiles))
            setMessage(BeatPrompterApplication.getResourceString(R.string.accessingCloudStorage, mCloudStorage.cloudStorageName))
            setCancelable(false)
            isIndeterminate = true
            show()
        }
    }

    override fun onCloudItemFound(cloudItem: CloudItemInfo) {
        // We're only interested in downloading files.
        if (cloudItem is CloudFileInfo) {
            mCloudFilesFound.add(cloudItem)
            if (!SongListActivity.mCachedCloudFiles.hasLatestVersionOf(cloudItem))
                mCloudFilesToDownload.add(cloudItem)
        }
    }

    override fun onFolderSearchError(t: Throwable) {
        mErrorOccurred = true
        mHandler.obtainMessage(EventHandler.CLOUD_SYNC_ERROR, t.message).sendToTarget()
    }

    override fun onFolderSearchComplete() {
        mCloudStorage.downloadFiles(mCloudFilesToDownload, this)
    }

    override fun onItemDownloaded(result: CloudDownloadResult) {
        if (result is SuccessfulCloudDownloadResult)
            SongListActivity.mCachedCloudFiles.add(CachedCloudFile.createCachedCloudFile(result))
        else
        // IMPLICIT if(result is FailedCloudDownloadResult)
            SongListActivity.mCachedCloudFiles.remove(result.mCloudFileInfo)
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