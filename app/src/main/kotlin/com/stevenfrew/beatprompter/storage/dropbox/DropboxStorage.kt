package com.stevenfrew.beatprompter.storage.dropbox

import androidx.fragment.app.Fragment
import com.dropbox.core.DbxException
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.oauth.DbxOAuthException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.GetMetadataErrorException
import com.dropbox.core.v2.files.ListFolderResult
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.storage.*
import com.stevenfrew.beatprompter.util.Utils
import io.reactivex.subjects.PublishSubject
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.FileOutputStream

/**
 * DropBox implementation of the storage system.
 */
class DropboxStorage(parentFragment: Fragment) :
	Storage(parentFragment, DROPBOX_CACHE_FOLDER_NAME) {

	private val requestConfig = DbxRequestConfig.newBuilder(BeatPrompter.APP_NAME)
		.build()

	override val directorySeparator: String
		get() = "/"

	override val cloudIconResourceId: Int
		get() = R.drawable.ic_dropbox

	override val cloudStorageName: String
		get() = BeatPrompter.getResourceString(R.string.dropbox_string)

	interface DropboxAction {
		fun onConnected(client: DbxClientV2)
		fun onAuthenticationRequired()
	}

	private fun isSuitableFileToDownload(filename: String): Boolean {
		return EXTENSIONS_TO_DOWNLOAD.contains(FilenameUtils.getExtension(filename))
	}

	private fun downloadFiles(
		client: DbxClientV2,
		listener: StorageListener,
		itemSource: PublishSubject<DownloadResult>,
		messageSource: PublishSubject<String>,
		filesToDownload: List<FileInfo>
	) {
		for (file in filesToDownload) {
			if (listener.shouldCancel())
				break
			try {
				val metadata = client.files().getMetadata(file.mID)
				val result = if (metadata is FileMetadata) {
					val title = file.mName
					Logger.log { "File title: $title" }
					messageSource.onNext(BeatPrompter.getResourceString(R.string.checking, title))
					val safeFilename = Utils.makeSafeFilename(title)
					val targetFile = File(cacheFolder, safeFilename)
					Logger.log { "Safe filename: $safeFilename" }

					Logger.log("Downloading now ...")
					messageSource.onNext(BeatPrompter.getResourceString(R.string.downloading, title))
					// Don't check lastModified ... ALWAYS download.
					if (listener.shouldCancel())
						break
					val localFile = downloadDropboxFile(client, metadata, targetFile)
					val updatedCloudFile = FileInfo(
						file.mID, metadata.name, metadata.serverModified,
						file.mSubfolderIDs
					)
					SuccessfulDownloadResult(updatedCloudFile, localFile)
				} else
					FailedDownloadResult(file)
				itemSource.onNext(result)
				if (listener.shouldCancel())
					break
			} catch (metadataException: GetMetadataErrorException) {
				if (metadataException.errorValue.pathValue.isNotFound)
					itemSource.onNext(FailedDownloadResult(file))
				else {
					itemSource.onError(metadataException)
					return
				}
			} catch (e: Exception) {
				itemSource.onError(e)
				return
			}

		}
		itemSource.onComplete()
	}

	private fun downloadDropboxFile(client: DbxClientV2, file: FileMetadata, localFile: File): File {
		val fos = FileOutputStream(localFile)
		fos.use { stream ->
			val downloader = client.files().download(file.id)
			downloader.use {
				it.download(stream)
			}
		}
		return localFile
	}

	private fun readFolderContents(
		client: DbxClientV2,
		folder: FolderInfo,
		listener: StorageListener,
		itemSource: PublishSubject<ItemInfo>,
		messageSource: PublishSubject<String>,
		recurseSubfolders: Boolean
	) {
		val foldersToSearch = ArrayList<FolderInfo>()
		foldersToSearch.add(folder)

		while (foldersToSearch.isNotEmpty()) {
			if (listener.shouldCancel())
				break
			val folderToSearch = foldersToSearch.removeAt(0)
			val currentFolderID = folderToSearch.mID
			val currentFolderName = folderToSearch.mName
			messageSource.onNext(
				BeatPrompter.getResourceString(
					R.string.scanningFolder,
					currentFolderName
				)
			)

			try {
				Logger.log("Getting list of everything in Dropbox folder.")
				var listResult: ListFolderResult? = client.files().listFolder(currentFolderID)
				while (listResult != null) {
					if (listener.shouldCancel())
						break
					val entries = listResult.entries
					for (metadata in entries) {
						if (listener.shouldCancel())
							break
						if (metadata is FileMetadata) {
							val filename = metadata.name.lowercase()
							if (isSuitableFileToDownload(filename))
								itemSource.onNext(
									FileInfo(
										metadata.id, metadata.name, metadata.serverModified,
										if (folderToSearch.mParentFolder == null) "" else currentFolderID
									)
								)
						} else if (metadata is FolderMetadata) {
							Logger.log("Adding folder to list of folders to query ...")
							val newFolder = FolderInfo(
								folderToSearch,
								metadata.getPathLower(),
								metadata.getName(),
								metadata.getPathDisplay()
							)
							if (recurseSubfolders)
								foldersToSearch.add(newFolder)
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

	private fun updateDropboxCredentials(cred: DbxCredential): DbxCredential {
		Preferences.dropboxAccessToken = cred.accessToken
		Preferences.dropboxRefreshToken = cred.refreshToken
		Preferences.dropboxExpiryTime = cred.expiresAt
		return cred
	}

	private fun getStoredDropboxCredentials(): DbxCredential? {
		val storedAccessToken = Preferences.dropboxAccessToken
		val storedRefreshToken = Preferences.dropboxRefreshToken
		val storedExpiryTime = Preferences.dropboxExpiryTime
		if (storedAccessToken != null && storedRefreshToken != null && storedExpiryTime != 0L) {
			val cred = DbxCredential(
				storedAccessToken,
				storedExpiryTime,
				storedRefreshToken,
				BeatPrompter.APP_NAME,
				DROPBOX_APP_KEY
			)
			return if (cred.aboutToExpire()) {
				try {
					val refreshResult = cred.refresh(requestConfig)
					val newCred = DbxCredential(
						refreshResult.accessToken,
						refreshResult.expiresAt,
						cred.refreshToken,
						BeatPrompter.APP_NAME,
						DROPBOX_APP_KEY
					)
					updateDropboxCredentials(newCred)
				} catch (authEx: DbxOAuthException) {
					return null
				} catch (ex: DbxException) {
					return null
				}
			} else cred
		}
		return null
	}

	private fun doDropboxAction(action: DropboxAction) {
		// Did we authenticate last time it failed?
		val authCred = Auth.getDbxCredential()
		val storedCred = getStoredDropboxCredentials()
		val cred = storedCred ?: authCred
		if (cred == null) {
			action.onAuthenticationRequired()
			Auth.startOAuth2PKCE(
				mParentFragment.requireContext(), DROPBOX_APP_KEY, requestConfig
			)
			return
		}
		updateDropboxCredentials(cred)
		action.onConnected(DbxClientV2(requestConfig, cred.accessToken))
	}

	override fun downloadFiles(
		filesToRefresh: List<FileInfo>,
		storageListener: StorageListener,
		itemSource: PublishSubject<DownloadResult>,
		messageSource: PublishSubject<String>
	) {
		doDropboxAction(object : DropboxAction {
			override fun onConnected(client: DbxClientV2) {
				downloadFiles(client, storageListener, itemSource, messageSource, filesToRefresh)
			}

			override fun onAuthenticationRequired() {
				storageListener.onAuthenticationRequired()
			}
		})
	}

	public override fun readFolderContents(
		folder: FolderInfo,
		listener: StorageListener,
		itemSource: PublishSubject<ItemInfo>,
		messageSource: PublishSubject<String>,
		recurseSubfolders: Boolean
	) {
		doDropboxAction(object : DropboxAction {
			override fun onConnected(client: DbxClientV2) {
				readFolderContents(client, folder, listener, itemSource, messageSource, recurseSubfolders)
			}

			override fun onAuthenticationRequired() {
				listener.onAuthenticationRequired()
			}
		})
	}

	override fun getRootPath(listener: StorageListener, rootPathSource: PublishSubject<FolderInfo>) {
		rootPathSource.onNext(FolderInfo("", DROPBOX_ROOT_PATH, DROPBOX_ROOT_PATH))
	}

	companion object {

		private const val DROPBOX_CACHE_FOLDER_NAME = "dropbox"

		@Suppress("SpellCheckingInspection")
		private const val DROPBOX_APP_KEY = "hay1puzmg41f02r"
		private const val DROPBOX_ROOT_PATH = "/"

		private val EXTENSIONS_TO_DOWNLOAD = hashSetOf(
			"txt",
			"mp3",
			"wav",
			"m4a",
			"aac",
			"ogg",
			"png",
			"jpg",
			"bmp",
			"tif",
			"tiff",
			"jpeg",
			"jpe",
			"pcx"
		)
	}
}
