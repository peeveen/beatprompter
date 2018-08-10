package com.stevenfrew.beatprompter.cloud

import android.app.Activity
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.SongList
import com.stevenfrew.beatprompter.cloud.demo.DemoCloudStorage
import com.stevenfrew.beatprompter.cloud.dropbox.DropboxCloudStorage
import com.stevenfrew.beatprompter.cloud.googledrive.GoogleDriveCloudStorage
import com.stevenfrew.beatprompter.cloud.onedrive.OneDriveCloudStorage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

abstract class CloudStorage protected constructor(protected var mParentActivity: Activity, cloudCacheFolderName: String) {
    var cacheFolder: CloudCacheFolder
        protected set

    abstract val cloudStorageName: String

    abstract val directorySeparator: String

    abstract val cloudIconResourceId: Int

    init {
        cacheFolder = CloudCacheFolder(SongList.mBeatPrompterSongFilesFolder!!, cloudCacheFolderName)
        if (!cacheFolder.exists())
            if (!cacheFolder.mkdir())
                Log.e(BeatPrompterApplication.TAG, "Failed to create cloud cache folder.")
    }

    fun constructFullPath(folderPath: String, itemName: String): String {
        var fullPath = folderPath
        if (!fullPath.endsWith(directorySeparator))
            fullPath += directorySeparator
        return fullPath + itemName
    }

    fun downloadFiles(filesToRefresh: MutableList<CloudFileInfo>, listener: CloudItemDownloadListener) {
        for (defaultCloudDownload in SongList.mDefaultCloudDownloads)
            if (filesToRefresh.contains(defaultCloudDownload.mCloudFileInfo))
                filesToRefresh.remove(defaultCloudDownload.mCloudFileInfo)

        val disp = CompositeDisposable()
        val downloadSource = PublishSubject.create<CloudDownloadResult>()
        disp.add(downloadSource.subscribe({ listener.onItemDownloaded(it) }, { listener.onDownloadError(it) }, { listener.onDownloadComplete() }))
        val messageSource = PublishSubject.create<String>()
        disp.add(messageSource.subscribe { listener.onProgressMessageReceived(it) })
        try {
            // Always include the temporary set list and default midi alias files.
            for (defaultCloudDownload in SongList.mDefaultCloudDownloads)
                downloadSource.onNext(defaultCloudDownload)
            downloadFiles(filesToRefresh, listener, downloadSource, messageSource)
        } finally {
            // TODO: figure out how/when to dispose of this
            //            disp.dispose();
        }
    }

    fun readFolderContents(folder: CloudFolderInfo, listener: CloudFolderSearchListener, includeSubfolders: Boolean, returnFolders: Boolean) {
        val disp = CompositeDisposable()
        val folderContentsSource = PublishSubject.create<CloudItemInfo>()
        disp.add(folderContentsSource.subscribe({ listener.onCloudItemFound(it) }, { listener.onFolderSearchError(it) }, { listener.onFolderSearchComplete() }))
        val messageSource = PublishSubject.create<String>()
        disp.add(messageSource.subscribe { listener.onProgressMessageReceived(it) })
        try {
            for (defaultCloudDownload in SongList.mDefaultCloudDownloads)
                folderContentsSource.onNext(defaultCloudDownload.mCloudFileInfo)
            readFolderContents(folder, listener, folderContentsSource, messageSource, includeSubfolders, returnFolders)
        } finally {
            // TODO: figure out how/when to dispose of this
            //            disp.dispose();
        }
    }

    fun selectFolder(parentActivity: Activity, listener: CloudFolderSelectionListener) {
        try {
            getRootPath(object : CloudRootPathListener {
                override fun onRootPathFound(rootPath: CloudFolderInfo) {
                    val dialog = ChooseCloudFolderDialog(parentActivity, this@CloudStorage, listener, rootPath)
                    dialog.showDialog()
                }

                override fun onRootPathError(t: Throwable) {
                    listener.onFolderSelectedError(t)
                }

                override fun onAuthenticationRequired() {
                    listener.onAuthenticationRequired()
                }

                override fun shouldCancel(): Boolean {
                    return listener.shouldCancel()
                }
            })
        } catch (e: Exception) {
            listener.onFolderSelectedError(e)
        }

    }

    private fun getRootPath(listener: CloudRootPathListener) {
        val disp = CompositeDisposable()
        val rootPathSource = PublishSubject.create<CloudFolderInfo>()
        disp.add(rootPathSource.subscribe({ listener.onRootPathFound(it) }, { listener.onRootPathError(it) }))
        try {
            getRootPath(listener, rootPathSource)
        } finally {
            // TODO: figure out how/when to dispose of this
            //            disp.dispose();
        }
    }

    protected abstract fun getRootPath(listener: CloudListener, rootPathSource: PublishSubject<CloudFolderInfo>)

    protected abstract fun downloadFiles(filesToRefresh: List<CloudFileInfo>, cloudListener: CloudListener, itemSource: PublishSubject<CloudDownloadResult>, messageSource: PublishSubject<String>)

    protected abstract fun readFolderContents(folder: CloudFolderInfo, listener: CloudListener, itemSource: PublishSubject<CloudItemInfo>, messageSource: PublishSubject<String>, includeSubfolders: Boolean, returnFolders: Boolean)

    companion object {

        fun getInstance(cloudType: CloudType, parentActivity: Activity): CloudStorage {
            if (cloudType === CloudType.Dropbox)
                return DropboxCloudStorage(parentActivity)
            if (cloudType === CloudType.OneDrive)
                return OneDriveCloudStorage(parentActivity)
            return if (cloudType === CloudType.GoogleDrive) GoogleDriveCloudStorage(parentActivity) else DemoCloudStorage(parentActivity)
        }
    }

}