package com.stevenfrew.beatprompter.storage.googledrive

import android.content.Intent
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.drive.DriveScopes
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.storage.DownloadResult
import com.stevenfrew.beatprompter.storage.FailedDownloadResult
import com.stevenfrew.beatprompter.storage.FileInfo
import com.stevenfrew.beatprompter.storage.FolderInfo
import com.stevenfrew.beatprompter.storage.ItemInfo
import com.stevenfrew.beatprompter.storage.Storage
import com.stevenfrew.beatprompter.storage.StorageListener
import com.stevenfrew.beatprompter.storage.StorageType
import com.stevenfrew.beatprompter.storage.SuccessfulDownloadResult
import com.stevenfrew.beatprompter.ui.pref.FileSettingsFragment
import com.stevenfrew.beatprompter.util.CoroutineTask
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.execute
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Date
import kotlin.coroutines.CoroutineContext

/**
 * GoogleDrive implementation of the storage system.
 */
class GoogleDriveStorage(parentFragment: Fragment) :
	Storage(parentFragment, StorageType.GoogleDrive) {

	private val mGoogleClientSignInOptions =
		GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
			.requestScopes(Scope(DriveScopes.DRIVE_READONLY), Scope(Scopes.EMAIL))
			.build()
	private val mGoogleSignInClient =
		GoogleSignIn.getClient(mParentFragment.requireActivity(), mGoogleClientSignInOptions)

	override val cloudStorageName: String
		get() = BeatPrompter.appResources.getString(R.string.google_drive_string)

	override val directorySeparator: String
		get() = "/"

	override val cloudIconResourceId: Int
		get() = R.drawable.ic_google_drive

	internal interface GoogleDriveAction {
		fun onConnected(client: com.google.api.services.drive.Drive)
		fun onAuthenticationRequired()
	}

	private fun <T> authorize(
		intent: Intent,
		itemSource: PublishSubject<T>,
		onAuthenticated: GoogleDriveAction?,
	) {
		FileSettingsFragment.mOnGoogleDriveAuthenticated = {
			onAuthenticated?.let { doGoogleDriveAction(itemSource, it) }
		}
		FileSettingsFragment.mOnGoogleDriveAuthenticationFailed = {
			itemSource.onError(Exception(mParentFragment.getString(R.string.googleDriveAccessFailed)))
		}
		(mParentFragment as FileSettingsFragment).mGoogleDriveAuthenticator?.launch(intent)
	}

	private fun <T> doGoogleDriveAction(itemSource: PublishSubject<T>, action: GoogleDriveAction) {
		try {
			val context = mParentFragment.requireContext()
			var alreadySignedInAccount =
				GoogleSignIn.getLastSignedInAccount(context)
			if (alreadySignedInAccount?.email == null) {
				mGoogleSignInClient.signOut()
				alreadySignedInAccount = null
			}
			if (alreadySignedInAccount == null) {
				authorize(
					mGoogleSignInClient.signInIntent,
					itemSource,
					action
				)
			} else {
				val credential = GoogleAccountCredential.usingOAuth2(
					context, listOf(*SCOPES)
				)
					.setSelectedAccount(alreadySignedInAccount.account)
					.setBackOff(ExponentialBackOff())
				val transport = NetHttpTransport()
				val jsonFactory = GsonFactory()
				val service = com.google.api.services.drive.Drive.Builder(
					transport, jsonFactory, credential
				)
					.setApplicationName(BeatPrompter.APP_NAME)
					.build()
				action.onConnected(service)
			}
		} catch (t: Throwable) {
			itemSource.onError(t)
		}
	}

	private class ReadGoogleDriveFolderContentsTask(
		val mClient: com.google.api.services.drive.Drive,
		val mStorage: GoogleDriveStorage,
		val mFolder: FolderInfo,
		val mListener: StorageListener,
		val mItemSource: PublishSubject<ItemInfo>,
		val mMessageSource: PublishSubject<String>,
		val mRecurseFolders: Boolean,
		val mReAuthenticationFn: (Intent) -> Unit
	) : CoroutineTask<Unit, Unit, Unit> {
		override val coroutineContext: CoroutineContext
			get() = Dispatchers.IO

		override fun onPreExecute() {
			// Do nothing.
		}

		override fun onError(t: Throwable) = mItemSource.onError(t)

		override fun onProgressUpdate(progress: Unit) {
			// Do nothing.
		}

		override fun onPostExecute(result: Unit) {
			// Do nothing.
		}

		override fun doInBackground(params: Unit, progressUpdater: suspend (Unit) -> Unit) {
			val foldersToQuery = ArrayList<FolderInfo>()
			foldersToQuery.add(mFolder)

			var firstFolder = true
			while (foldersToQuery.isNotEmpty()) {
				if (mListener.shouldCancel())
					break
				val currentFolder = foldersToQuery.removeAt(0)
				val currentFolderID = currentFolder.mID
				val currentFolderName = currentFolder.mName
				mMessageSource.onNext(
					BeatPrompter.appResources.getString(
						R.string.scanningFolder,
						if (firstFolder) "..." else currentFolderName
					)
				)
				firstFolder = false

				val queryString = "trashed=false and '$currentFolderID' in parents"
				try {
					if (mListener.shouldCancel())
						break
					val request = mClient.files().list().setQ(queryString)
						.setFields("nextPageToken,files(${GOOGLE_DRIVE_REQUESTED_FILE_FIELDS_SCAN})")
					do {
						if (mListener.shouldCancel())
							break
						val children = request.execute()

						Logger.log("Iterating through contents, seeing what needs updated/downloaded/deleted ...")

						for (child in children.files) {
							if (mListener.shouldCancel())
								break
							// Ignore shortcuts
							val resolvedChild = if (child.shortcutDetails != null)
								mClient.files().get(child.shortcutDetails.targetId)
									.setFields(GOOGLE_DRIVE_REQUESTED_FILE_FIELDS_SCAN).execute()
							else
								child
							val fileID = resolvedChild.id
							val title = resolvedChild.name
							val mimeType = resolvedChild.mimeType
							Logger.log { "File ID: $fileID" }
							if (GOOGLE_DRIVE_FOLDER_MIMETYPE == mimeType) {
								val newFolder = FolderInfo(
									currentFolder,
									fileID,
									title,
									mStorage.constructFullPath(
										currentFolder.mDisplayPath,
										title
									)
								)
								if (mRecurseFolders) {
									Logger.log("Adding folder to list of folders to query ...")
									foldersToQuery.add(newFolder)
								}
								mItemSource.onNext(newFolder)
							} else {
								Logger.log { "File title: $title" }
								val modifiedTime = Date(resolvedChild.modifiedTime.value)
								val newFile = FileInfo(
									fileID, title, modifiedTime,
									if (currentFolder.mParentFolder == null) "" else currentFolderID
								)
								mItemSource.onNext(newFile)
							}
						}
						request.pageToken = children.nextPageToken
						if (mListener.shouldCancel())
							break
					} while (request.pageToken != null && request.pageToken.isNotEmpty())
				} catch (authException: UserRecoverableAuthIOException) {
					try {
						mReAuthenticationFn(authException.intent)
					} catch (reAuthException: Exception) {
						mItemSource.onError(reAuthException)
					}
				}
			}
			mItemSource.onComplete()
		}
	}

	private inner class DownloadGoogleDriveFilesTask(
		val mClient: com.google.api.services.drive.Drive,
		val mListener: StorageListener,
		val mItemSource: PublishSubject<DownloadResult>,
		val mMessageSource: PublishSubject<String>,
		val mFilesToDownload: List<FileInfo>,
		val mDownloadFolder: File
	) : CoroutineTask<Unit, Unit, Unit> {
		override val coroutineContext: CoroutineContext
			get() = Dispatchers.IO

		override fun onPreExecute() {
			// Do nothing.
		}

		override fun onError(t: Throwable) = mItemSource.onError(t)

		override fun onProgressUpdate(progress: Unit) {
			// Do nothing.
		}

		override fun onPostExecute(result: Unit) {
			// Do nothing.
		}

		override fun doInBackground(params: Unit, progressUpdater: suspend (Unit) -> Unit) =
			this@GoogleDriveStorage.downloadFiles(
				mFilesToDownload,
				mListener,
				mItemSource,
				mMessageSource
			) {
				try {
					val file = mClient.files().get(it.mID)
						.setFields(GOOGLE_DRIVE_REQUESTED_FILE_FIELDS_DOWNLOAD).execute()
					if (!file.trashed) {
						val title = file.name
						Logger.log { "File title: $title" }
						val safeFilename = Utils.makeSafeFilename(it.mID)
						Logger.log { "Safe filename: $safeFilename" }
						Logger.log("Downloading now ...")
						if (!mListener.shouldCancel()) {
							val localFile = downloadGoogleDriveFile(file, safeFilename)
							val updatedCloudFile = FileInfo(
								it.mID, file.name, Date(file.modifiedTime.value),
								it.mSubfolderIDs
							)
							SuccessfulDownloadResult(updatedCloudFile, localFile)
						} else
							FailedDownloadResult(it)
					} else
						FailedDownloadResult(it)
				} catch (jsonException: GoogleJsonResponseException) {
					// You get a 404 if the document has been 100% deleted.
					if (jsonException.statusCode == 404)
						FailedDownloadResult(it)
					else
						throw jsonException
				}
			}

		private fun downloadGoogleDriveFile(
			file: com.google.api.services.drive.model.File,
			filename: String
		): File {
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

	override fun getRootPath(listener: StorageListener, rootPathSource: PublishSubject<FolderInfo>) =
		rootPathSource.onNext(
			FolderInfo(
				GOOGLE_DRIVE_ROOT_FOLDER_ID,
				GOOGLE_DRIVE_ROOT_PATH,
				GOOGLE_DRIVE_ROOT_PATH
			)
		)

	override fun downloadFiles(
		filesToRefresh: List<FileInfo>,
		storageListener: StorageListener,
		itemSource: PublishSubject<DownloadResult>,
		messageSource: PublishSubject<String>
	) {
		doGoogleDriveAction(itemSource, object : GoogleDriveAction {
			override fun onConnected(client: com.google.api.services.drive.Drive) {
				DownloadGoogleDriveFilesTask(
					client,
					storageListener,
					itemSource,
					messageSource,
					filesToRefresh,
					cacheFolder
				).execute(Unit)
			}

			override fun onAuthenticationRequired() = storageListener.onAuthenticationRequired()
		})
	}

	override fun readFolderContents(
		folder: FolderInfo,
		listener: StorageListener,
		itemSource: PublishSubject<ItemInfo>,
		messageSource: PublishSubject<String>,
		recurseSubFolders: Boolean
	) {
		doGoogleDriveAction(itemSource, object : GoogleDriveAction {
			override fun onConnected(client: com.google.api.services.drive.Drive) {
				ReadGoogleDriveFolderContentsTask(
					client,
					this@GoogleDriveStorage,
					folder,
					listener,
					itemSource,
					messageSource,
					recurseSubFolders
				) { intent ->
					authorize(intent, itemSource, null)
				}.execute(Unit)
			}

			override fun onAuthenticationRequired() = listener.onAuthenticationRequired()
		})
	}

	companion object {
		private const val GOOGLE_DRIVE_ROOT_FOLDER_ID = "root"
		private const val GOOGLE_DRIVE_ROOT_PATH = "/"
		const val GOOGLE_DRIVE_CACHE_FOLDER_NAME = "google_drive"
		private const val GOOGLE_DRIVE_REQUESTED_FILE_FIELDS_COMMON = "id,name,mimeType,modifiedTime"
		private const val GOOGLE_DRIVE_REQUESTED_FILE_FIELDS_SCAN =
			"${GOOGLE_DRIVE_REQUESTED_FILE_FIELDS_COMMON},shortcutDetails"
		private const val GOOGLE_DRIVE_REQUESTED_FILE_FIELDS_DOWNLOAD =
			"${GOOGLE_DRIVE_REQUESTED_FILE_FIELDS_COMMON},trashed"
		private const val GOOGLE_DRIVE_FOLDER_MIMETYPE = "application/vnd.google-apps.folder"
		private val SCOPES = arrayOf(DriveScopes.DRIVE_READONLY, Scopes.EMAIL)
	}
}
