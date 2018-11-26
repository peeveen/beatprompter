package com.stevenfrew.beatprompter.cloud.onedrive

import android.app.Activity
import android.os.AsyncTask
import android.util.Log
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
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.cloud.*
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList

class OneDriveCloudStorage(parentActivity: Activity) : CloudStorage(parentActivity, ONEDRIVE_CACHE_FOLDER_NAME) {

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
        get() = BeatPrompterApplication.getResourceString(R.string.onedrive_string)

    interface OneDriveAction {
        fun onConnected(client: IOneDriveClient)
        fun onAuthenticationRequired()
    }

    private class GetOneDriveFolderContentsTask internal constructor(internal var mClient: IOneDriveClient, internal var mCloudStorage: OneDriveCloudStorage, internal var mFolder: CloudFolderInfo, internal var mListener: CloudListener, internal var mItemSource: PublishSubject<CloudItemInfo>, internal var mMessageSource: PublishSubject<String>, internal var mIncludeSubfolders: Boolean, internal var mReturnFolders: Boolean) : AsyncTask<Void, Void, Void>() {
        private fun isSuitableFileToDownload(childItem: Item): Boolean {
            return childItem.audio != null || childItem.image != null || childItem.name.toLowerCase().endsWith(".txt")
        }

        override fun doInBackground(vararg args: Void): Void? {
            val folders = ArrayList<CloudFolderInfo>()
            folders.add(mFolder)

            while (!folders.isEmpty()) {
                if (mListener.shouldCancel())
                    break
                val nextFolder = folders.removeAt(0)
                val currentFolderID = nextFolder.mID
                mMessageSource.onNext(BeatPrompterApplication.getResourceString(R.string.scanningFolder, nextFolder.mName))

                try {
                    Log.d(BeatPrompterApplication.TAG, "Getting list of everything in OneDrive folder.")
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
                                    mItemSource.onNext(CloudFileInfo(child.id, child.name, child.lastModifiedDateTime.time,
                                            if (nextFolder.mParentFolder == null) null else nextFolder.mName))
                            } else if (child.folder != null) {
                                val fullPath = mCloudStorage.constructFullPath(nextFolder.mDisplayPath, child.name)
                                val newFolder = CloudFolderInfo(nextFolder, child.id, child.name, fullPath)
                                if (mIncludeSubfolders) {
                                    Log.d(BeatPrompterApplication.TAG, "Adding folder to list of folders to query ...")
                                    folders.add(newFolder)
                                }
                                if (mReturnFolders)
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

    private class DownloadOneDriveFilesTask internal constructor(internal var mClient: IOneDriveClient, internal var mListener: CloudListener, internal var mItemSource: PublishSubject<CloudDownloadResult>, internal var mMessageSource: PublishSubject<String>, internal var mFilesToDownload: List<CloudFileInfo>, internal var mDownloadFolder: File) : AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg args: Void): Void? {
            for (file in mFilesToDownload) {
                if (mListener.shouldCancel())
                    break
                var result: CloudDownloadResult
                try {
                    val driveFile = mClient.drive.getItems(file.mID).buildRequest().get()
                    if (driveFile != null) {
                        val title = file.mName
                        Log.d(BeatPrompterApplication.TAG, "File title: $title")
                        mMessageSource.onNext(BeatPrompterApplication.getResourceString(R.string.checking, title))
                        val safeFilename = Utils.makeSafeFilename(title)
                        val targetFile = File(mDownloadFolder, safeFilename)
                        Log.d(BeatPrompterApplication.TAG, "Safe filename: $safeFilename")

                        Log.d(BeatPrompterApplication.TAG, "Downloading now ...")
                        mMessageSource.onNext(BeatPrompterApplication.getResourceString(R.string.downloading, title))
                        // Don't check lastModified ... ALWAYS download.
                        if (mListener.shouldCancel())
                            break
                        val localFile = downloadOneDriveFile(mClient, driveFile, targetFile)
                        val updatedCloudFile=CloudFileInfo(file.mID,driveFile.name,driveFile.lastModifiedDateTime.time,
                                file.mSubfolder)
                        result = SuccessfulCloudDownloadResult(updatedCloudFile, localFile)
                    } else
                        result = FailedCloudDownloadResult(file)
                    mItemSource.onNext(result)
                    if (mListener.shouldCancel())
                        break
                } catch (odse: OneDriveServiceException) {
                    if (odse.isError(OneDriveErrorCodes.ItemNotFound)) {
                        result = FailedCloudDownloadResult(file)
                        mItemSource.onNext(result)
                    } else {
                        mItemSource.onError(odse)
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

        @Throws(IOException::class)
        private fun downloadOneDriveFile(client: IOneDriveClient, file: Item, localFile: File): File {
            val fos = FileOutputStream(localFile)
            fos.use {
                val inputStream = client.drive.getItems(file.id).content.buildRequest().get()
                inputStream.use { inStream->
                    Utils.streamToStream(inStream, fos)
                }
            }
            return localFile
        }
    }

    private fun doOneDriveAction(action: OneDriveAction) {
        val callback = object : ICallback<IOneDriveClient> {
            override fun success(clientResult: IOneDriveClient) {
                Log.v(BeatPrompterApplication.TAG, "Signed in to OneDrive")
                action.onConnected(clientResult)
            }

            override fun failure(error: ClientException) {
                Log.e(BeatPrompterApplication.TAG, "Nae luck signing in to OneDrive")
                action.onAuthenticationRequired()
            }
        }

        val oneDriveConfig = DefaultClientConfig.createWithAuthenticator(oneDriveAuthenticator)
        OneDriveClient.Builder()
                .fromConfig(oneDriveConfig)
                .loginAndBuildClient(mParentActivity, callback)
    }

    private class GetOneDriveRootFolderTask internal constructor(internal var mClient: IOneDriveClient) : AsyncTask<Void, Void, CloudFolderInfo>() {
        override fun doInBackground(vararg args: Void): CloudFolderInfo {
            val rootFolder = mClient.drive.root.buildRequest().get()
            return CloudFolderInfo(rootFolder.id, ONEDRIVE_ROOT_PATH, ONEDRIVE_ROOT_PATH)
        }
    }

    override fun getRootPath(listener: CloudListener, rootPathSource: PublishSubject<CloudFolderInfo>) {
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
                rootPathSource.onError(CloudException(BeatPrompterApplication.getResourceString(R.string.could_not_find_cloud_root_error)))
            }
        })
    }

    override fun downloadFiles(filesToRefresh: List<CloudFileInfo>, cloudListener: CloudListener, itemSource: PublishSubject<CloudDownloadResult>, messageSource: PublishSubject<String>) {
        doOneDriveAction(object : OneDriveAction {
            override fun onConnected(client: IOneDriveClient) {
                try {
                    DownloadOneDriveFilesTask(client, cloudListener, itemSource, messageSource, filesToRefresh, cacheFolder).execute()
                } catch (e: Exception) {
                    itemSource.onError(e)
                }

            }

            override fun onAuthenticationRequired() {
                cloudListener.onAuthenticationRequired()
            }
        })
    }

    public override fun readFolderContents(folder: CloudFolderInfo, listener: CloudListener, itemSource: PublishSubject<CloudItemInfo>, messageSource: PublishSubject<String>, includeSubfolders: Boolean, returnFolders: Boolean) {
        doOneDriveAction(object : OneDriveAction {
            override fun onConnected(client: IOneDriveClient) {
                try {
                    GetOneDriveFolderContentsTask(client, this@OneDriveCloudStorage, folder, listener, itemSource, messageSource, includeSubfolders, returnFolders).execute()
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
