package com.stevenfrew.beatprompter.cloud

import android.app.Activity
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.ui.SongListActivity
import com.stevenfrew.beatprompter.cloud.demo.DemoCloudStorage
import com.stevenfrew.beatprompter.cloud.dropbox.DropboxCloudStorage
import com.stevenfrew.beatprompter.cloud.googledrive.GoogleDriveCloudStorage
import com.stevenfrew.beatprompter.cloud.local.LocalStorage
import com.stevenfrew.beatprompter.cloud.onedrive.OneDriveCloudStorage
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

abstract class CloudStorage protected constructor(protected var mParentActivity: Activity, cloudCacheFolderName: String) {
    // TODO: Figure out when to call dispose on this.
    private val mCompositeDisposable = CompositeDisposable()

    var cacheFolder: CloudCacheFolder
        protected set

    abstract val cloudStorageName: String

    abstract val directorySeparator: String

    abstract val cloudIconResourceId: Int

    init {
        cacheFolder = CloudCacheFolder(SongListActivity.mBeatPrompterSongFilesFolder!!, cloudCacheFolderName)
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
        for (defaultCloudDownload in SongListActivity.mDefaultCloudDownloads)
            if (filesToRefresh.contains(defaultCloudDownload.mCloudFileInfo))
                filesToRefresh.remove(defaultCloudDownload.mCloudFileInfo)

        val downloadSource = PublishSubject.create<CloudDownloadResult>()
        mCompositeDisposable.add(downloadSource.subscribe({ listener.onItemDownloaded(it) }, { listener.onDownloadError(it) }, { listener.onDownloadComplete() }))
        val messageSource = PublishSubject.create<String>()
        mCompositeDisposable.add(messageSource.subscribe { listener.onProgressMessageReceived(it) })
        // Always include the temporary set list and default midi alias files.
        for (defaultCloudDownload in SongListActivity.mDefaultCloudDownloads)
            downloadSource.onNext(defaultCloudDownload)
        downloadFiles(filesToRefresh, listener, downloadSource, messageSource)
    }

    fun readFolderContents(folder: CloudFolderInfo, listener: CloudFolderSearchListener, includeSubfolders: Boolean, returnFolders: Boolean) {
        val folderContentsSource = PublishSubject.create<CloudItemInfo>()
        mCompositeDisposable.add(folderContentsSource.subscribe({ listener.onCloudItemFound(it) }, { listener.onFolderSearchError(it) }, { listener.onFolderSearchComplete() }))
        val messageSource = PublishSubject.create<String>()
        mCompositeDisposable.add(messageSource.subscribe { listener.onProgressMessageReceived(it) })
        for (defaultCloudDownload in SongListActivity.mDefaultCloudDownloads)
            folderContentsSource.onNext(defaultCloudDownload.mCloudFileInfo)
        readFolderContents(folder, listener, folderContentsSource, messageSource, includeSubfolders, returnFolders)
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
        val rootPathSource = PublishSubject.create<CloudFolderInfo>()
        mCompositeDisposable.add(rootPathSource.subscribe({ listener.onRootPathFound(it) }, { listener.onRootPathError(it) }))
        getRootPath(listener, rootPathSource)
    }

    protected abstract fun getRootPath(listener: CloudListener, rootPathSource: PublishSubject<CloudFolderInfo>)

    protected abstract fun downloadFiles(filesToRefresh: List<CloudFileInfo>, cloudListener: CloudListener, itemSource: PublishSubject<CloudDownloadResult>, messageSource: PublishSubject<String>)

    protected abstract fun readFolderContents(folder: CloudFolderInfo, listener: CloudListener, itemSource: PublishSubject<CloudItemInfo>, messageSource: PublishSubject<String>, includeSubfolders: Boolean, returnFolders: Boolean)

    companion object {

        fun getInstance(cloudType: CloudType, parentActivity: Activity): CloudStorage {
            return when {
                cloudType === CloudType.Dropbox -> DropboxCloudStorage(parentActivity)
                cloudType === CloudType.OneDrive -> OneDriveCloudStorage(parentActivity)
                cloudType === CloudType.GoogleDrive -> GoogleDriveCloudStorage(parentActivity)
                cloudType === CloudType.Local -> LocalStorage(parentActivity)
                else -> DemoCloudStorage(parentActivity)
            }
        }
    }

}