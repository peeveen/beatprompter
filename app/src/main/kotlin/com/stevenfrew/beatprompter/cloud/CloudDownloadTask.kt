package com.stevenfrew.beatprompter.cloud

import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Handler
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.SongList
import com.stevenfrew.beatprompter.cache.CachedCloudFile
import java.util.ArrayList

class CloudDownloadTask(private val mCloudStorage: CloudStorage, private val mHandler: Handler, private val mCloudPath: String, private val mIncludeSubFolders: Boolean, filesToUpdate: ArrayList<CachedCloudFile>?) : AsyncTask<String, String, Boolean>(), CloudFolderSearchListener, CloudItemDownloadListener {
    private var mProgressDialog: ProgressDialog? = null
    private var mErrorOccurred = false
    private var mFilesToUpdate: MutableList<CloudFileInfo>
    private val mCloudFilesFound = ArrayList<CloudFileInfo>()
    private val mCloudFilesToDownload = ArrayList<CloudFileInfo>()

    private val isRefreshingSelectedFiles: Boolean
        get() = !mFilesToUpdate.isEmpty()

    init {
        mFilesToUpdate = filesToUpdate?.map { ftu -> CloudFileInfo(ftu.mID, ftu.mName, ftu.mLastModified, ftu.mSubfolder) }?.toMutableList() ?: ArrayList()
    }

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
        mProgressDialog = ProgressDialog(SongList.mSongListInstance)
        mProgressDialog!!.setTitle(BeatPrompterApplication.getResourceString(R.string.downloadingFiles))
        mProgressDialog!!.setMessage(BeatPrompterApplication.getResourceString(R.string.accessingCloudStorage, mCloudStorage.cloudStorageName))
        mProgressDialog!!.setCancelable(false)
        mProgressDialog!!.isIndeterminate = true
        mProgressDialog!!.show()
    }

    override fun onCloudItemFound(cloudItem: CloudItemInfo) {
        // We're only interested in downloading files.
        if (cloudItem is CloudFileInfo) {
            mCloudFilesFound.add(cloudItem)
            if (!SongList.mCachedCloudFiles.hasLatestVersionOf(cloudItem))
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
            SongList.mCachedCloudFiles.add(CachedCloudFile.createCachedCloudFile(result))
        else
        // IMPLICIT if(result is FailedCloudDownloadResult)
            SongList.mCachedCloudFiles.remove(result.mCloudFileInfo)
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
            SongList.mCachedCloudFiles.removeNonExistent(mCloudFilesFound.map { c -> c.mID }.toSet())
        mHandler.obtainMessage(EventHandler.CACHE_UPDATED, SongList.mCachedCloudFiles).sendToTarget()
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