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
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.getHash
import com.stevenfrew.beatprompter.util.toHashString
import io.reactivex.subjects.PublishSubject
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.FileOutputStream

/**
 * DropBox implementation of the storage system.
 */
class DropboxStorage(parentFragment: Fragment) :
	Storage(parentFragment, StorageType.Dropbox) {

	private val hashBuffer = ByteArray(HASH_BUFFER_SIZE)

	private val requestConfig = DbxRequestConfig.newBuilder(BeatPrompter.APP_NAME)
		.build()

	override val directorySeparator: String
		get() = "/"

	override val cloudIconResourceId: Int
		get() = R.drawable.ic_dropbox

	override val cloudStorageName: String
		get() = BeatPrompter.appResources.getString(R.string.dropbox_string)

	interface DropboxAction {
		fun onConnected(client: DbxClientV2)
		fun onAuthenticationRequired()
	}

	private fun isSuitableFileToDownload(filename: String): Boolean =
		EXTENSIONS_TO_DOWNLOAD.contains(FilenameUtils.getExtension(filename))

	private fun downloadFiles(
		client: DbxClientV2,
		listener: StorageListener,
		itemSource: PublishSubject<DownloadResult>,
		messageSource: PublishSubject<String>,
		filesToDownload: List<FileInfo>
	) = downloadFiles(filesToDownload, listener, itemSource, messageSource) {
		try {
			val metadata = client.files().getMetadata(it.id)
			if (metadata is FileMetadata) {
				val title = it.name
				Logger.log({ "File title: $title" })
				val safeFilename = Utils.makeSafeFilename(title)
				val targetFile = File(cacheFolder, safeFilename)
				Logger.log({ "Safe filename: $safeFilename" })

				Logger.log("Downloading now ...")
				// Don't check lastModified ... ALWAYS download.
				if (!listener.shouldCancel()) {
					val localFile = downloadDropboxFile(client, metadata, targetFile)
					val updatedCloudFile = FileInfo(
						it.id, metadata.name, metadata.serverModified,
						metadata.contentHash ?: "",
						it.subfolderIds
					)
					SuccessfulDownloadResult(updatedCloudFile, localFile)
				} else
					FailedDownloadResult(it)
			} else
				FailedDownloadResult(it)
		} catch (metadataException: GetMetadataErrorException) {
			if (metadataException.errorValue.pathValue.isNotFound)
				FailedDownloadResult(it)
			else
				throw metadataException
		}
	}

	fun ByteArray.getSha256Hash() = getHash("SHA-256")

	fun File.getDropboxHash(): String {
		val inputStream = this.inputStream()
		var sha256s = mutableListOf<ByteArray>()
		while (true) {
			val bytesRead = inputStream.read(hashBuffer)
			if (bytesRead > 0) {
				val bytes = if (bytesRead == HASH_BUFFER_SIZE) hashBuffer else hashBuffer.copyOfRange(
					0,
					bytesRead
				)
				sha256s.add(bytes.getSha256Hash())
			}
			if (bytesRead != HASH_BUFFER_SIZE)
				break
		}
		val allBytes = sha256s.reduce { a1, a2 -> a1 + a2 }
		val finalHash = allBytes.getSha256Hash()
		val finalHashString = finalHash.toHashString(64)
		return finalHashString
	}

	private fun downloadDropboxFile(client: DbxClientV2, file: FileMetadata, localFile: File): File =
		localFile.also {
			// If we already have the file, we must be downloading cos the database
			// was corrupted. The existing file might be valid.
			if (!localFile.exists() || localFile.getDropboxHash() != file.contentHash)
				FileOutputStream(it).use { stream ->
					client.files().download(file.id).use { downloader ->
						downloader.download(stream)
					}
				}
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
			val currentFolderID = folderToSearch.id
			val currentFolderName = folderToSearch.name
			messageSource.onNext(
				BeatPrompter.appResources.getString(
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
										if (folderToSearch.parentFolder == null) "" else currentFolderID
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

	private fun updateDropboxCredentials(cred: DbxCredential): DbxCredential =
		cred.also {
			BeatPrompter.preferences.dropboxAccessToken = it.accessToken
			BeatPrompter.preferences.dropboxRefreshToken = it.refreshToken
			BeatPrompter.preferences.dropboxExpiryTime = it.expiresAt
		}

	private fun getStoredDropboxCredentials(): DbxCredential? {
		val storedAccessToken = BeatPrompter.preferences.dropboxAccessToken
		val storedRefreshToken = BeatPrompter.preferences.dropboxRefreshToken
		val storedExpiryTime = BeatPrompter.preferences.dropboxExpiryTime
		if (storedAccessToken.isNotBlank() && storedRefreshToken.isNotBlank() && storedExpiryTime != 0L) {
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
				} catch (_: DbxOAuthException) {
					return null
				} catch (_: DbxException) {
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
				parentFragment.requireContext(), DROPBOX_APP_KEY, requestConfig
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
			override fun onConnected(client: DbxClientV2) =
				downloadFiles(client, storageListener, itemSource, messageSource, filesToRefresh)

			override fun onAuthenticationRequired() = storageListener.onAuthenticationRequired()
		})
	}

	public override fun readFolderContents(
		folder: FolderInfo,
		listener: StorageListener,
		itemSource: PublishSubject<ItemInfo>,
		messageSource: PublishSubject<String>,
		recurseSubFolders: Boolean
	) {
		doDropboxAction(object : DropboxAction {
			override fun onConnected(client: DbxClientV2) =
				readFolderContents(client, folder, listener, itemSource, messageSource, recurseSubFolders)

			override fun onAuthenticationRequired() = listener.onAuthenticationRequired()
		})
	}

	override fun getRootPath(listener: StorageListener, rootPathSource: PublishSubject<FolderInfo>) {
		rootPathSource.onNext(FolderInfo("", DROPBOX_ROOT_PATH, DROPBOX_ROOT_PATH))
	}

	companion object {
		const val DROPBOX_CACHE_FOLDER_NAME = "dropbox"
		private const val HASH_BUFFER_SIZE = 4 * 1024 * 1024

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
