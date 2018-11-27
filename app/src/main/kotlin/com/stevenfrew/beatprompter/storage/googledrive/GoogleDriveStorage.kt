package com.stevenfrew.beatprompter.storage.googledrive

import android.app.Activity
import android.content.IntentSender
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.drive.Drive
import com.google.android.gms.plus.Plus
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.drive.DriveScopes
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ui.SongListActivity
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.storage.*
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

class GoogleDriveStorage(parentActivity: Activity) : Storage(parentActivity, GOOGLE_DRIVE_CACHE_FOLDER_NAME) {


    override val cloudStorageName: String
        get() = BeatPrompterApplication.getResourceString(R.string.google_drive_string)

    override val directorySeparator: String
        get() = "/"

    override val cloudIconResourceId: Int
        get() = R.drawable.ic_google_drive

    internal interface GoogleDriveAction {
        fun onConnected(client: com.google.api.services.drive.Drive)
        fun onAuthenticationRequired()
    }

    internal inner class GoogleDriveConnectionListener(private var mAction: GoogleDriveAction) : GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        private var mClient: GoogleApiClient? = null
        fun setClient(client: GoogleApiClient) {
            mClient = client
        }

        override fun onConnectionFailed(result: ConnectionResult) {
            // Called whenever the API client fails to connect.
            Log.i(BeatPrompterApplication.TAG, "GoogleApiClient connection failed: " + result.toString())
            mAction.onAuthenticationRequired()
            if (!result.hasResolution()) {
                // show the localized error dialog.
                GoogleApiAvailability.getInstance().getErrorDialog(SongListActivity.mSongListInstance, result.errorCode, 0).show()
            } else {
                // The failure has a resolution. Resolve it.
                // Called typically when the app is not yet authorized, and an
                // authorization
                // dialog is displayed to the user.
                try {
                    Log.i(BeatPrompterApplication.TAG, "GoogleApiClient starting connection resolution ...")
                    result.startResolutionForResult(SongListActivity.mSongListInstance, GoogleDriveStorage.REQUEST_CODE_RESOLUTION)
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(BeatPrompterApplication.TAG, "Exception while starting resolution activity", e)
                }

            }
        }

        override fun onConnected(connectionHint: Bundle?) {
            Log.i(BeatPrompterApplication.TAG, "API client connected.")

            val accountName = Plus.AccountApi.getAccountName(mClient)
            val credential = GoogleAccountCredential.usingOAuth2(
                    mParentActivity, Arrays.asList(*SCOPES))
                    .setSelectedAccountName(accountName)
                    .setBackOff(ExponentialBackOff())
            val transport = AndroidHttp.newCompatibleTransport()
            val jsonFactory = JacksonFactory.getDefaultInstance()
            val service = com.google.api.services.drive.Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName(BeatPrompterApplication.APP_NAME)
                    .build()
            mAction.onConnected(service)
        }

        override fun onConnectionSuspended(cause: Int) {
            Log.i(BeatPrompterApplication.TAG, "GoogleApiClient connection suspended")
        }
    }

    private fun doGoogleDriveAction(action: GoogleDriveAction) {
        val listener = GoogleDriveConnectionListener(action)
        val googleApiClient = GoogleApiClient.Builder(mParentActivity)
                .addApi(Drive.API)
                .addApi(Plus.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(listener)
                .addOnConnectionFailedListener(listener)
                .build()
        listener.setClient(googleApiClient)
        googleApiClient.connect()
    }

    private class ReadGoogleDriveFolderContentsTask internal constructor(internal var mClient: com.google.api.services.drive.Drive, internal var mStorage: GoogleDriveStorage, internal var mFolder: FolderInfo, internal var mListener: StorageListener, internal var mItemSource: PublishSubject<ItemInfo>, internal var mMessageSource: PublishSubject<String>, internal var mIncludeSubfolders: Boolean, internal var mReturnFolders: Boolean) : AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg args: Void): Void? {
            val foldersToQuery = ArrayList<FolderInfo>()
            foldersToQuery.add(mFolder)

            var firstFolder = true
            while (!foldersToQuery.isEmpty()) {
                if (mListener.shouldCancel())
                    break
                val currentFolder = foldersToQuery.removeAt(0)
                val currentFolderID = currentFolder.mID
                val currentFolderName = currentFolder.mName
                mMessageSource.onNext(BeatPrompterApplication.getResourceString(R.string.scanningFolder, if (firstFolder) "..." else currentFolderName))
                firstFolder = false

                var queryString = "trashed=false and '$currentFolderID' in parents"
                if (!mIncludeSubfolders && !mReturnFolders)
                    queryString += " and mimeType != '$GOOGLE_DRIVE_FOLDER_MIMETYPE'"
                try {
                    if (mListener.shouldCancel())
                        break
                    val request = mClient.files().list().setQ(queryString).setFields("nextPageToken,files(id,name,mimeType,modifiedTime)")
                    do {
                        if (mListener.shouldCancel())
                            break
                        val children = request.execute()

                        Log.d(BeatPrompterApplication.TAG, "Iterating through contents, seeing what needs updated/downloaded/deleted ...")

                        for (child in children.files) {
                            if (mListener.shouldCancel())
                                break
                            val fileID = child.id
                            val title = child.name
                            Log.d(BeatPrompterApplication.TAG, "File ID: $fileID")
                            val mimeType = child.mimeType
                            if (GOOGLE_DRIVE_FOLDER_MIMETYPE == mimeType) {
                                val newFolder = FolderInfo(currentFolder, fileID, title, mStorage.constructFullPath(currentFolder.mDisplayPath, title))
                                if (mIncludeSubfolders) {
                                    Log.d(BeatPrompterApplication.TAG, "Adding folder to list of folders to query ...")
                                    foldersToQuery.add(newFolder)
                                }
                                if (mReturnFolders)
                                    mItemSource.onNext(newFolder)
                            } else {
                                Log.d(BeatPrompterApplication.TAG, "File title: $title")
                                val newFile = FileInfo(fileID, title, Date(child.modifiedTime.value),
                                        if (currentFolder.mParentFolder == null) null else currentFolderName)
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

    private class DownloadGoogleDriveFilesTask internal constructor(internal var mClient: com.google.api.services.drive.Drive, internal var mListener: StorageListener, internal var mItemSource: PublishSubject<DownloadResult>, internal var mMessageSource: PublishSubject<String>, internal var mFilesToDownload: List<FileInfo>, internal var mDownloadFolder: File) : AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg args: Void): Void? {
            for (cloudFile in mFilesToDownload) {
                if (mListener.shouldCancel())
                    break
                var result: DownloadResult
                try {
                    val file = mClient.files().get(cloudFile.mID).setFields("id,name,mimeType,trashed,modifiedTime").execute()
                    result = if (!file.trashed) {
                        val title = file.name
                        Log.d(BeatPrompterApplication.TAG, "File title: $title")
                        val safeFilename = Utils.makeSafeFilename(cloudFile.mID)
                        Log.d(BeatPrompterApplication.TAG, "Safe filename: $safeFilename")
                        Log.d(BeatPrompterApplication.TAG, "Downloading now ...")
                        mMessageSource.onNext(BeatPrompterApplication.getResourceString(R.string.downloading, title))
                        if (mListener.shouldCancel())
                            break
                        val localFile = downloadGoogleDriveFile(file, safeFilename)
                        val updatedCloudFile = FileInfo(cloudFile.mID, file.name, Date(file.modifiedTime.value),
                                cloudFile.mSubfolder)
                        SuccessfulDownloadResult(updatedCloudFile, localFile)
                    } else
                        FailedDownloadResult(cloudFile)
                    mItemSource.onNext(result)
                    if (mListener.shouldCancel())
                        break
                } catch (gjre: GoogleJsonResponseException) {
                    // You get a 404 if the document has been 100% deleted.
                    if (gjre.statusCode == 404) {
                        result = FailedDownloadResult(cloudFile)
                        mItemSource.onNext(result)
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

        @Throws(IOException::class)
        private fun downloadGoogleDriveFile(file: com.google.api.services.drive.model.File, filename: String): File {
            val localFile = File(mDownloadFolder, filename)
            val inputStream = getDriveFileInputStream(file)
            inputStream?.use { inStream ->
                Log.d(BeatPrompterApplication.TAG, "Creating new local file, " + localFile.absolutePath)
                val fos = FileOutputStream(localFile)
                fos.use {
                    Utils.streamToStream(inStream, it)
                }
            }
            return localFile
        }

        @Throws(IOException::class)
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

    override fun readFolderContents(folder: FolderInfo, listener: StorageListener, itemSource: PublishSubject<ItemInfo>, messageSource: PublishSubject<String>, includeSubfolders: Boolean, returnFolders: Boolean) {
        doGoogleDriveAction(object : GoogleDriveAction {
            override fun onConnected(client: com.google.api.services.drive.Drive) {
                ReadGoogleDriveFolderContentsTask(client, this@GoogleDriveStorage, folder, listener, itemSource, messageSource, includeSubfolders, returnFolders).execute()
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
        private val SCOPES = arrayOf(DriveScopes.DRIVE_READONLY)
        private const val REQUEST_CODE_RESOLUTION = 1
        private const val COMPLETE_AUTHORIZATION_REQUEST_CODE = 2

        internal fun recoverAuthorization(uraioe: UserRecoverableAuthIOException) {
            SongListActivity.mSongListInstance.startActivityForResult(uraioe.intent, COMPLETE_AUTHORIZATION_REQUEST_CODE)
        }
    }
}
