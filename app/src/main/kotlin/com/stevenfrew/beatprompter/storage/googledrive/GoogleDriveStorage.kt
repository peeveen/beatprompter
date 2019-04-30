package com.stevenfrew.beatprompter.storage.googledrive

import android.app.Activity
import android.os.AsyncTask
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.drive.DriveScopes
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.storage.*
import com.stevenfrew.beatprompter.ui.SongListActivity
import com.stevenfrew.beatprompter.util.Utils
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.*

/**
 * GoogleDrive implementation of the storage system.
 */
class GoogleDriveStorage(parentActivity: Activity)
    : Storage(parentActivity, GOOGLE_DRIVE_CACHE_FOLDER_NAME) {

    private val mGoogleClientSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(DriveScopes.DRIVE_READONLY), Scope(Scopes.EMAIL))
            .build()
    private val mGoogleSignInClient = GoogleSignIn.getClient(mParentActivity, mGoogleClientSignInOptions)

    override val cloudStorageName: String
        get() = BeatPrompter.getResourceString(R.string.google_drive_string)

    override val directorySeparator: String
        get() = "/"

    override val cloudIconResourceId: Int
        get() = R.drawable.ic_google_drive

    internal interface GoogleDriveAction {
        fun onConnected(client: com.google.api.services.drive.Drive)
        fun onAuthenticationRequired()
    }

    private fun doGoogleDriveAction(action: GoogleDriveAction) {
        var alreadySignedInAccount = GoogleSignIn.getLastSignedInAccount(mParentActivity)
        if (alreadySignedInAccount?.email == null) {
            mGoogleSignInClient.signOut()
            alreadySignedInAccount = null
        }
        if (alreadySignedInAccount == null) {
            mActionOnHold = action
            mParentActivity.startActivityForResult(mGoogleSignInClient.signInIntent, REQUEST_CODE_GOOGLE_SIGN_IN)
        } else {
            val credential = GoogleAccountCredential.usingOAuth2(
                    mParentActivity, Arrays.asList(*SCOPES))
                    .setSelectedAccount(alreadySignedInAccount.account)
                    .setBackOff(ExponentialBackOff())
            val transport = AndroidHttp.newCompatibleTransport()
            val jsonFactory = JacksonFactory.getDefaultInstance()
            val service = com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(BeatPrompter.APP_NAME)
                    .build()
            action.onConnected(service)
        }
    }

    private class ReadGoogleDriveFolderContentsTask internal constructor(internal val mClient: com.google.api.services.drive.Drive,
                                                                         internal val mStorage: GoogleDriveStorage,
                                                                         internal val mFolder: FolderInfo,
                                                                         internal val mListener: StorageListener,
                                                                         internal val mItemSource: PublishSubject<ItemInfo>,
                                                                         internal val mMessageSource: PublishSubject<String>,
                                                                         internal val mRecurseSubfolders: Boolean) : AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg args: Void): Void? {
            val foldersToQuery = ArrayList<FolderInfo>()
            foldersToQuery.add(mFolder)

            var firstFolder = true
            while (foldersToQuery.isNotEmpty()) {
                if (mListener.shouldCancel())
                    break
                val currentFolder = foldersToQuery.removeAt(0)
                val currentFolderID = currentFolder.mID
                val currentFolderName = currentFolder.mName
                mMessageSource.onNext(BeatPrompter.getResourceString(R.string.scanningFolder, if (firstFolder) "..." else currentFolderName))
                firstFolder = false

                val queryString = "trashed=false and '$currentFolderID' in parents"
                try {
                    if (mListener.shouldCancel())
                        break
                    val request = mClient.files().list().setQ(queryString).setFields("nextPageToken,files(id,name,mimeType,modifiedTime)")
                    do {
                        if (mListener.shouldCancel())
                            break
                        val children = request.execute()

                        Logger.log("Iterating through contents, seeing what needs updated/downloaded/deleted ...")

                        for (child in children.files) {
                            if (mListener.shouldCancel())
                                break
                            val fileID = child.id
                            val title = child.name
                            Logger.log { "File ID: $fileID" }
                            val mimeType = child.mimeType
                            if (GOOGLE_DRIVE_FOLDER_MIMETYPE == mimeType) {
                                val newFolder = FolderInfo(currentFolder, fileID, title, mStorage.constructFullPath(currentFolder.mDisplayPath, title))
                                if (mRecurseSubfolders) {
                                    Logger.log("Adding folder to list of folders to query ...")
                                    foldersToQuery.add(newFolder)
                                }
                                mItemSource.onNext(newFolder)
                            } else {
                                Logger.log { "File title: $title" }
                                val newFile = FileInfo(fileID, title, Date(child.modifiedTime.value),
                                        if (currentFolder.mParentFolder == null) "" else currentFolderID)
                                mItemSource.onNext(newFile)
                            }
                        }
                        request.pageToken = children.nextPageToken
                        if (mListener.shouldCancel())
                            break
                    } while (request.pageToken != null && request.pageToken.isNotEmpty())
                } catch (uraioe: UserRecoverableAuthIOException) {
                    recoverAuthorization(uraioe)
                } catch (e: Exception) {
                    mItemSource.onError(e)
                    return null
                }

            }
            mItemSource.onComplete()
            return null
        }
    }

    private class DownloadGoogleDriveFilesTask internal constructor(internal val mClient: com.google.api.services.drive.Drive,
                                                                    internal val mListener: StorageListener,
                                                                    internal val mItemSource: PublishSubject<DownloadResult>,
                                                                    internal val mMessageSource: PublishSubject<String>,
                                                                    internal val mFilesToDownload: List<FileInfo>,
                                                                    internal val mDownloadFolder: File) : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg args: Void): Void? {
            for (cloudFile in mFilesToDownload) {
                if (mListener.shouldCancel())
                    break
                try {
                    val file = mClient.files().get(cloudFile.mID).setFields("id,name,mimeType,trashed,modifiedTime").execute()
                    val result = if (!file.trashed) {
                        val title = file.name
                        Logger.log { "File title: $title" }
                        val safeFilename = Utils.makeSafeFilename(cloudFile.mID)
                        Logger.log { "Safe filename: $safeFilename" }
                        Logger.log("Downloading now ...")
                        mMessageSource.onNext(BeatPrompter.getResourceString(R.string.downloading, title))
                        if (mListener.shouldCancel())
                            break
                        val localFile = downloadGoogleDriveFile(file, safeFilename)
                        val updatedCloudFile = FileInfo(cloudFile.mID, file.name, Date(file.modifiedTime.value),
                                cloudFile.mSubfolderIDs)
                        SuccessfulDownloadResult(updatedCloudFile, localFile)
                    } else
                        FailedDownloadResult(cloudFile)
                    mItemSource.onNext(result)
                    if (mListener.shouldCancel())
                        break
                } catch (gjre: GoogleJsonResponseException) {
                    // You get a 404 if the document has been 100% deleted.
                    if (gjre.statusCode == 404) {
                        mItemSource.onNext(FailedDownloadResult(cloudFile))
                    } else {
                        mItemSource.onError(gjre)
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

        private fun downloadGoogleDriveFile(file: com.google.api.services.drive.model.File, filename: String): File {
            val localFile = File(mDownloadFolder, filename)
            val inputStream = getDriveFileInputStream(file)
            inputStream?.use { inStream ->
                Logger.log { "Creating new local file, ${localFile.absolutePath}" }
                val fos = FileOutputStream(localFile)
                fos.use {
                    Utils.streamToStream(inStream, it)
                }
            }
            return localFile
        }

        private fun getDriveFileInputStream(file: com.google.api.services.drive.model.File): InputStream? {
            val isGoogleDoc = file.mimeType.startsWith("application/vnd.google-apps.")
            if (isGoogleDoc) {
                val isGoogleTextDoc = file.mimeType == "application/vnd.google-apps.document"
                if (isGoogleTextDoc)
                    return mClient.files().export(file.id, "text/plain").executeMediaAsInputStream()
                // Ignore spreadsheets, drawings, etc.
            } else
            // Binary files.
                return mClient.files().get(file.id).executeMediaAsInputStream()
            return null
        }

    }

    override fun getRootPath(listener: StorageListener, rootPathSource: PublishSubject<FolderInfo>) {
        rootPathSource.onNext(FolderInfo(GOOGLE_DRIVE_ROOT_FOLDER_ID, GOOGLE_DRIVE_ROOT_PATH, GOOGLE_DRIVE_ROOT_PATH))
    }

    override fun downloadFiles(filesToRefresh: List<FileInfo>, storageListener: StorageListener, itemSource: PublishSubject<DownloadResult>, messageSource: PublishSubject<String>) {
        doGoogleDriveAction(object : GoogleDriveAction {
            override fun onConnected(client: com.google.api.services.drive.Drive) {
                DownloadGoogleDriveFilesTask(client, storageListener, itemSource, messageSource, filesToRefresh, cacheFolder).execute()
            }

            override fun onAuthenticationRequired() {
                storageListener.onAuthenticationRequired()
            }
        })
    }

    override fun readFolderContents(folder: FolderInfo, listener: StorageListener, itemSource: PublishSubject<ItemInfo>, messageSource: PublishSubject<String>, recurseSubfolders: Boolean) {
        doGoogleDriveAction(object : GoogleDriveAction {
            override fun onConnected(client: com.google.api.services.drive.Drive) {
                ReadGoogleDriveFolderContentsTask(client, this@GoogleDriveStorage, folder, listener, itemSource, messageSource, recurseSubfolders).execute()
            }

            override fun onAuthenticationRequired() {
                listener.onAuthenticationRequired()
            }
        })
    }

    companion object {
        private const val GOOGLE_DRIVE_ROOT_FOLDER_ID = "root"
        private const val GOOGLE_DRIVE_ROOT_PATH = "/"
        private const val GOOGLE_DRIVE_CACHE_FOLDER_NAME = "google_drive"
        private const val GOOGLE_DRIVE_FOLDER_MIMETYPE = "application/vnd.google-apps.folder"
        private val SCOPES = arrayOf(DriveScopes.DRIVE_READONLY, Scopes.EMAIL)
        private const val COMPLETE_AUTHORIZATION_REQUEST_CODE = 2

        private var mActionOnHold: GoogleDriveAction? = null

        const val REQUEST_CODE_GOOGLE_SIGN_IN = 1541

        internal fun recoverAuthorization(uraioe: UserRecoverableAuthIOException) {
            SongListActivity.mSongListInstance.startActivityForResult(uraioe.intent, COMPLETE_AUTHORIZATION_REQUEST_CODE)
        }

        fun completeAction(parentActivity: Activity) {
            val actionOnHold = mActionOnHold
            mActionOnHold = null
            if (actionOnHold != null)
                GoogleDriveStorage(parentActivity).doGoogleDriveAction(actionOnHold)
        }
    }
}
