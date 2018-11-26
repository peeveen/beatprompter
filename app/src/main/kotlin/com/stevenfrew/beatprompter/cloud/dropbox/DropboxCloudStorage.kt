package com.stevenfrew.beatprompter.cloud.dropbox

import android.app.Activity
import android.util.Log
import com.dropbox.core.DbxException
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.GetMetadataErrorException
import com.dropbox.core.v2.files.ListFolderResult
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.cloud.*
import io.reactivex.subjects.PublishSubject
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class DropboxCloudStorage(parentActivity: Activity) : CloudStorage(parentActivity, DROPBOX_CACHE_FOLDER_NAME) {

    override val directorySeparator: String
        get() = "/"

    override val cloudIconResourceId: Int
        get() = R.drawable.ic_dropbox

    override val cloudStorageName: String
        get() = BeatPrompterApplication.getResourceString(R.string.dropbox_string)

    interface DropboxAction {
        fun onConnected(client: DbxClientV2)
        fun onAuthenticationRequired()
    }

    private fun isSuitableFileToDownload(filename: String): Boolean {
        return EXTENSIONS_TO_DOWNLOAD.contains(FilenameUtils.getExtension(filename))
    }

    private fun downloadFiles(client: DbxClientV2, listener: CloudListener, itemSource: PublishSubject<CloudDownloadResult>, messageSource: PublishSubject<String>, filesToDownload: List<CloudFileInfo>) {
        for (file in filesToDownload) {
            if (listener.shouldCancel())
                break
            var result: CloudDownloadResult
            try {
                val mdata = client.files().getMetadata(file.mID)
                if (mdata is FileMetadata) {
                    val title = file.mName
                    Log.d(BeatPrompterApplication.TAG, "File title: $title")
                    messageSource.onNext(BeatPrompterApplication.getResourceString(R.string.checking, title))
                    val safeFilename = Utils.makeSafeFilename(title)
                    val targetFile = File(cacheFolder, safeFilename)
                    Log.d(BeatPrompterApplication.TAG, "Safe filename: $safeFilename")

                    Log.d(BeatPrompterApplication.TAG, "Downloading now ...")
                    messageSource.onNext(BeatPrompterApplication.getResourceString(R.string.downloading, title))
                    // Don't check lastModified ... ALWAYS download.
                    if (listener.shouldCancel())
                        break
                    val localFile = downloadDropboxFile(client, mdata, targetFile)
                    val updatedCloudFile=CloudFileInfo(file.mID,mdata.name,mdata.serverModified,
                            file.mSubfolder)
                    result = SuccessfulCloudDownloadResult(updatedCloudFile, localFile)
                } else
                    result = FailedCloudDownloadResult(file)
                itemSource.onNext(result)
                if (listener.shouldCancel())
                    break
            } catch (gmee: GetMetadataErrorException) {
                if (gmee.errorValue.pathValue.isNotFound) {
                    result = FailedCloudDownloadResult(file)
                    itemSource.onNext(result)
                } else {
                    itemSource.onError(gmee)
                    return
                }
            } catch (e: Exception) {
                itemSource.onError(e)
                return
            }

        }
        itemSource.onComplete()
    }

    @Throws(IOException::class, DbxException::class)
    private fun downloadDropboxFile(client: DbxClientV2, file: FileMetadata, localfile: File): File {
        val fos = FileOutputStream(localfile)
        fos.use {
            val downloader = client.files().download(file.id)
            downloader.use {dler->
                dler.download(it)
            }
        }
        return localfile
    }

    private fun readFolderContents(client: DbxClientV2, folder: CloudFolderInfo, listener: CloudListener, itemSource: PublishSubject<CloudItemInfo>, messageSource: PublishSubject<String>, includeSubfolders: Boolean, returnFolders: Boolean) {
        val foldersToSearch = ArrayList<CloudFolderInfo>()
        foldersToSearch.add(folder)

        while (!foldersToSearch.isEmpty()) {
            if (listener.shouldCancel())
                break
            val folderToSearch = foldersToSearch.removeAt(0)
            val currentFolderID = folderToSearch.mID
            val currentFolderName = folderToSearch.mName
            messageSource.onNext(BeatPrompterApplication.getResourceString(R.string.scanningFolder, currentFolderName))

            try {
                Log.d(BeatPrompterApplication.TAG, "Getting list of everything in Dropbox folder.")
                var listResult: ListFolderResult? = client.files().listFolder(currentFolderID)
                while (listResult != null) {
                    if (listener.shouldCancel())
                        break
                    val entries = listResult.entries
                    for (mdata in entries) {
                        if (listener.shouldCancel())
                            break
                        if (mdata is FileMetadata) {
                            val filename = mdata.name.toLowerCase()
                            if (isSuitableFileToDownload(filename))
                                itemSource.onNext(CloudFileInfo(mdata.id, mdata.name, mdata.serverModified,
                                        if (folderToSearch.mParentFolder == null) null else currentFolderName))
                        } else if (mdata is FolderMetadata) {
                            Log.d(BeatPrompterApplication.TAG, "Adding folder to list of folders to query ...")
                            val newFolder = CloudFolderInfo(folderToSearch, mdata.getPathLower(), mdata.getName(), mdata.getPathDisplay())
                            if (includeSubfolders)
                                foldersToSearch.add(newFolder)
                            if (returnFolders)
                                itemSource.onNext(newFolder)
                        }
                    }
                    if (listener.shouldCancel())
                        break
                    listResult = if (listResult.hasMore)
                        client.files().listFolderContinue(listResult.cursor)
                    else
                        null
                }
            } catch (de: DbxException) {
                itemSource.onError(de)
                return
            }

        }
        itemSource.onComplete()
    }

    private fun doDropboxAction(action: DropboxAction) {
        val sharedPrefs = BeatPrompterApplication.privatePreferences
        var storedAccessToken = sharedPrefs.getString(BeatPrompterApplication.getResourceString(R.string.pref_dropboxAccessToken_key), null)
        if (storedAccessToken == null) {
            // Did we authenticate last time it failed?
            storedAccessToken = Auth.getOAuth2Token()
            if (storedAccessToken != null)
                sharedPrefs.edit().putString(BeatPrompterApplication.getResourceString(R.string.pref_dropboxAccessToken_key), storedAccessToken).apply()
        }
        if (storedAccessToken == null) {
            action.onAuthenticationRequired()
            Auth.startOAuth2Authentication(mParentActivity, DROPBOX_APP_KEY)
        } else {
            val requestConfig = DbxRequestConfig.newBuilder(BeatPrompterApplication.APP_NAME)
                    .build()
            action.onConnected(DbxClientV2(requestConfig, storedAccessToken))
        }
    }

    override fun downloadFiles(filesToRefresh: List<CloudFileInfo>, cloudListener: CloudListener, itemSource: PublishSubject<CloudDownloadResult>, messageSource: PublishSubject<String>) {
        doDropboxAction(object : DropboxAction {
            override fun onConnected(client: DbxClientV2) {
                downloadFiles(client, cloudListener, itemSource, messageSource, filesToRefresh)
            }

            override fun onAuthenticationRequired() {
                cloudListener.onAuthenticationRequired()
            }
        })
    }

    public override fun readFolderContents(folder: CloudFolderInfo, listener: CloudListener, itemSource: PublishSubject<CloudItemInfo>, messageSource: PublishSubject<String>, includeSubfolders: Boolean, returnFolders: Boolean) {
        doDropboxAction(object : DropboxAction {
            override fun onConnected(client: DbxClientV2) {
                readFolderContents(client, folder, listener, itemSource, messageSource, includeSubfolders, returnFolders)
            }

            override fun onAuthenticationRequired() {
                listener.onAuthenticationRequired()
            }
        })
    }

    override fun getRootPath(listener: CloudListener, rootPathSource: PublishSubject<CloudFolderInfo>) {
        rootPathSource.onNext(CloudFolderInfo("", DROPBOX_ROOT_PATH, DROPBOX_ROOT_PATH))
    }

    companion object {

        private const val DROPBOX_CACHE_FOLDER_NAME = "dropbox"
        private const val DROPBOX_APP_KEY = "hay1puzmg41f02r"
        private const val DROPBOX_ROOT_PATH = "/"

        private val EXTENSIONS_TO_DOWNLOAD = hashSetOf("txt", "mp3", "wav", "m4a", "aac", "ogg", "png", "jpg", "bmp", "tif", "tiff", "jpeg", "jpe", "pcx")
    }
}
