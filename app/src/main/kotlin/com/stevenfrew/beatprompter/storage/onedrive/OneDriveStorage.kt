package com.stevenfrew.beatprompter.storage.onedrive

import android.app.Activity
import android.os.AsyncTask
import com.onedrive.sdk.authentication.MSAAuthenticator
import com.onedrive.sdk.concurrency.ICallback
import com.onedrive.sdk.core.ClientException
import com.onedrive.sdk.core.DefaultClientConfig
import com.onedrive.sdk.core.OneDriveErrorCodes
import com.onedrive.sdk.extensions.IItemCollectionPage
import com.onedrive.sdk.extensions.IOneDriveClient
import com.onedrive.sdk.extensions.Item
import com.onedrive.sdk.extensions.OneDriveClient
import com.onedrive.sdk.http.OneDriveServiceException
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.storage.*
import com.stevenfrew.beatprompter.util.Utils
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * OneDrive implementation of the storage system.
 */
class OneDriveStorage(parentActivity: Activity)
    : Storage(parentActivity, ONEDRIVE_CACHE_FOLDER_NAME) {

    private val oneDriveAuthenticator = object : MSAAuthenticator() {
        override fun getClientId(): String {
            return ONEDRIVE_CLIENT_ID
        }

        override fun getScopes(): Array<String> {
            return arrayOf("onedrive.readonly", "wl.offline_access")
        }
    }

    override val directorySeparator: String
        get() = "/"

    override val cloudIconResourceId: Int
        get() = R.drawable.ic_onedrive

    override val cloudStorageName: String
        get() = BeatPrompter.getResourceString(R.string.onedrive_string)

    interface OneDriveAction {
        fun onConnected(client: IOneDriveClient)
        fun onAuthenticationRequired()
    }

    private class GetOneDriveFolderContentsTask constructor(val mClient: IOneDriveClient,
                                                            val mStorage: OneDriveStorage,
                                                            val mFolder: FolderInfo,
                                                            val mListener: StorageListener,
                                                            val mItemSource: PublishSubject<ItemInfo>,
                                                            val mMessageSource: PublishSubject<String>,
                                                            val mRecurseSubfolders: Boolean) : AsyncTask<Void, Void, Void>() {
        private fun isSuitableFileToDownload(childItem: Item): Boolean {
            return childItem.audio != null || childItem.image != null || childItem.name.toLowerCase(Locale.getDefault()).endsWith(".txt")
        }

        override fun doInBackground(vararg args: Void): Void? {
            val folders = ArrayList<FolderInfo>()
            folders.add(mFolder)

            while (folders.isNotEmpty()) {
                if (mListener.shouldCancel())
                    break
                val nextFolder = folders.removeAt(0)
                val currentFolderID = nextFolder.mID
                mMessageSource.onNext(BeatPrompter.getResourceString(R.string.scanningFolder, nextFolder.mName))

                try {
                    Logger.log("Getting list of everything in OneDrive folder.")
                    var page: IItemCollectionPage? = mClient.drive.getItems(currentFolderID).children.buildRequest().get()
                    while (page != null) {
                        if (mListener.shouldCancel())
                            break
                        val children = page.currentPage
                        for (child in children) {
                            if (mListener.shouldCancel())
                                break
                            if (child.file != null) {
                                if (isSuitableFileToDownload(child))
                                    mItemSource.onNext(FileInfo(child.id, child.name, child.lastModifiedDateTime.time,
                                            if (nextFolder.mParentFolder == null) "" else nextFolder.mID))
                            } else if (child.folder != null) {
                                val fullPath = mStorage.constructFullPath(nextFolder.mDisplayPath, child.name)
                                val newFolder = FolderInfo(nextFolder, child.id, child.name, fullPath)
                                if (mRecurseSubfolders) {
                                    Logger.log("Adding folder to list of folders to query ...")
                                    folders.add(newFolder)
                                }
                                mItemSource.onNext(newFolder)
                            }
                        }
                        if (mListener.shouldCancel())
                            break
                        val builder = page.nextPage
                        page = builder?.buildRequest()?.get()
                    }
                } catch (e: Exception) {
                    mItemSource.onError(e)
                    return null
                }

            }
            mItemSource.onComplete()
            return null
        }
    }

    private class DownloadOneDriveFilesTask constructor(var mClient: IOneDriveClient,
                                                        var mListener: StorageListener,
                                                        var mItemSource: PublishSubject<DownloadResult>,
                                                        var mMessageSource: PublishSubject<String>,
                                                        var mFilesToDownload: List<FileInfo>,
                                                        var mDownloadFolder: File) : AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg args: Void): Void? {
            for (file in mFilesToDownload) {
                if (mListener.shouldCancel())
                    break
                var result: DownloadResult
                try {
                    val driveFile = mClient.drive.getItems(file.mID).buildRequest().get()
                    if (driveFile != null) {
                        val title = file.mName
                        Logger.log { "File title: $title" }
                        mMessageSource.onNext(BeatPrompter.getResourceString(R.string.checking, title))
                        val safeFilename = Utils.makeSafeFilename(title)
                        val targetFile = File(mDownloadFolder, safeFilename)
                        Logger.log { "Safe filename: $safeFilename" }

                        Logger.log("Downloading now ...")
                        mMessageSource.onNext(BeatPrompter.getResourceString(R.string.downloading, title))
                        // Don't check lastModified ... ALWAYS download.
                        if (mListener.shouldCancel())
                            break
                        val localFile = downloadOneDriveFile(mClient, driveFile, targetFile)
                        val updatedCloudFile = FileInfo(file.mID, driveFile.name, driveFile.lastModifiedDateTime.time,
                                file.mSubfolderIDs)
                        result = SuccessfulDownloadResult(updatedCloudFile, localFile)
                    } else
                        result = FailedDownloadResult(file)
                    mItemSource.onNext(result)
                    if (mListener.shouldCancel())
                        break
                } catch (oneDriveException: OneDriveServiceException) {
                    if (oneDriveException.isError(OneDriveErrorCodes.ItemNotFound)) {
                        result = FailedDownloadResult(file)
                        mItemSource.onNext(result)
                    } else {
                        mItemSource.onError(oneDriveException)
                        return null
                    }
                } catch (e: Exception) {
                    mItemSource.onError(e)
                    return null
                }

            }
            mItemSource.onComplete()
            return null
        }

        private fun downloadOneDriveFile(client: IOneDriveClient, file: Item, localFile: File): File {
            val fos = FileOutputStream(localFile)
            fos.use {
                val inputStream = client.drive.getItems(file.id).content.buildRequest().get()
                inputStream.use { inStream ->
                    Utils.streamToStream(inStream, fos)
                }
            }
            return localFile
        }
    }

    private fun doOneDriveAction(action: OneDriveAction) {
        val callback = object : ICallback<IOneDriveClient> {
            override fun success(clientResult: IOneDriveClient) {
                Logger.log("Signed in to OneDrive")
                action.onConnected(clientResult)
            }

            override fun failure(error: ClientException) {
                Logger.log("Nae luck signing in to OneDrive")
                action.onAuthenticationRequired()
            }
        }

        val oneDriveConfig = DefaultClientConfig.createWithAuthenticator(oneDriveAuthenticator)
        OneDriveClient.Builder()
                .fromConfig(oneDriveConfig)
                .loginAndBuildClient(mParentActivity, callback)
    }

    private class GetOneDriveRootFolderTask constructor(var mClient: IOneDriveClient) : AsyncTask<Void, Void, FolderInfo>() {
        override fun doInBackground(vararg args: Void): FolderInfo {
            val rootFolder = mClient.drive.root.buildRequest().get()
            return FolderInfo(rootFolder.id, ONEDRIVE_ROOT_PATH, ONEDRIVE_ROOT_PATH)
        }
    }

    override fun getRootPath(listener: StorageListener, rootPathSource: PublishSubject<FolderInfo>) {
        doOneDriveAction(object : OneDriveAction {
            override fun onConnected(client: IOneDriveClient) {
                try {
                    val rootFolder = GetOneDriveRootFolderTask(client).execute().get()
                    rootPathSource.onNext(rootFolder)
                } catch (e: Exception) {
                    rootPathSource.onError(e)
                }

            }

            override fun onAuthenticationRequired() {
                rootPathSource.onError(StorageException(BeatPrompter.getResourceString(R.string.could_not_find_cloud_root_error)))
            }
        })
    }

    override fun downloadFiles(filesToRefresh: List<FileInfo>, storageListener: StorageListener, itemSource: PublishSubject<DownloadResult>, messageSource: PublishSubject<String>) {
        doOneDriveAction(object : OneDriveAction {
            override fun onConnected(client: IOneDriveClient) {
                try {
                    DownloadOneDriveFilesTask(client, storageListener, itemSource, messageSource, filesToRefresh, cacheFolder).execute()
                } catch (e: Exception) {
                    itemSource.onError(e)
                }

            }

            override fun onAuthenticationRequired() {
                storageListener.onAuthenticationRequired()
            }
        })
    }

    public override fun readFolderContents(folder: FolderInfo, listener: StorageListener, itemSource: PublishSubject<ItemInfo>, messageSource: PublishSubject<String>, recurseSubfolders: Boolean) {
        doOneDriveAction(object : OneDriveAction {
            override fun onConnected(client: IOneDriveClient) {
                try {
                    GetOneDriveFolderContentsTask(client, this@OneDriveStorage, folder, listener, itemSource, messageSource, recurseSubfolders).execute()
                } catch (e: Exception) {
                    itemSource.onError(e)
                }

            }

            override fun onAuthenticationRequired() {
                listener.onAuthenticationRequired()
            }
        })
    }

    companion object {
        private const val ONEDRIVE_CACHE_FOLDER_NAME = "onedrive"
        private const val ONEDRIVE_CLIENT_ID = "dc584873-700c-4377-98da-d088cca5c1f5" //This is your client ID
        private const val ONEDRIVE_ROOT_PATH = "/"
    }
}
