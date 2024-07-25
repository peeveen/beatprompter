package com.stevenfrew.beatprompter.storage.onedrive

import androidx.fragment.app.Fragment
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
import com.stevenfrew.beatprompter.storage.DownloadResult
import com.stevenfrew.beatprompter.storage.FailedDownloadResult
import com.stevenfrew.beatprompter.storage.FileInfo
import com.stevenfrew.beatprompter.storage.FolderInfo
import com.stevenfrew.beatprompter.storage.ItemInfo
import com.stevenfrew.beatprompter.storage.Storage
import com.stevenfrew.beatprompter.storage.StorageException
import com.stevenfrew.beatprompter.storage.StorageListener
import com.stevenfrew.beatprompter.storage.StorageType
import com.stevenfrew.beatprompter.storage.SuccessfulDownloadResult
import com.stevenfrew.beatprompter.util.CoroutineTask
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.execute
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext

/**
 * OneDrive implementation of the storage system.
 */
class OneDriveStorage(parentFragment: Fragment) :
	Storage(parentFragment, StorageType.OneDrive) {

	private val oneDriveAuthenticator = object : MSAAuthenticator() {
		override fun getClientId(): String = ONEDRIVE_CLIENT_ID
		override fun getScopes(): Array<String> = arrayOf("onedrive.readonly", "wl.offline_access")
	}

	override val directorySeparator: String
		get() = "/"

	override val cloudIconResourceId: Int
		get() = R.drawable.ic_onedrive

	override val cloudStorageName: String
		get() = BeatPrompter.appResources.getString(R.string.onedrive_string)

	interface OneDriveAction {
		fun onConnected(client: IOneDriveClient)
		fun onAuthenticationRequired()
	}

	private class GetOneDriveFolderContentsTask(
		val mClient: IOneDriveClient,
		val mStorage: OneDriveStorage,
		val mFolder: FolderInfo,
		val mListener: StorageListener,
		val mItemSource: PublishSubject<ItemInfo>,
		val mMessageSource: PublishSubject<String>,
		val mRecurseSubFolders: Boolean
	) : CoroutineTask<Unit, Unit, Unit> {
		override val coroutineContext: CoroutineContext
			get() = Dispatchers.IO

		override fun onPreExecute() {
			// Do nothing.
		}

		override fun onError(t: Throwable) = mItemSource.onError(t)

		override fun onProgressUpdate(progress: Unit) {
			// Do nothing. Listener will receive updates.
		}

		override fun onPostExecute(result: Unit) {
			// Do nothing.
		}

		override fun doInBackground(params: Unit, progressUpdater: suspend (Unit) -> Unit) {
			val folders = ArrayList<FolderInfo>()
			folders.add(mFolder)

			while (folders.isNotEmpty()) {
				if (mListener.shouldCancel())
					break
				val nextFolder = folders.removeAt(0)
				val currentFolderID = nextFolder.id
				mMessageSource.onNext(
					BeatPrompter.appResources.getString(
						R.string.scanningFolder,
						nextFolder.name
					)
				)

				Logger.log("Getting list of everything in OneDrive folder.")
				var page: IItemCollectionPage? =
					mClient.drive.getItems(currentFolderID).children.buildRequest().get()
				while (page != null) {
					if (mListener.shouldCancel())
						break
					val children = page.currentPage
					for (child in children) {
						if (mListener.shouldCancel())
							break
						if (child.file != null) {
							if (isSuitableFileToDownload(child))
								mItemSource.onNext(
									FileInfo(
										child.id, child.name, child.lastModifiedDateTime.time,
										if (nextFolder.parentFolder == null) "" else nextFolder.id
									)
								)
						} else if (child.folder != null) {
							val fullPath = mStorage.constructFullPath(nextFolder.displayPath, child.name)
							val newFolder = FolderInfo(nextFolder, child.id, child.name, fullPath)
							if (mRecurseSubFolders) {
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
			}
			mItemSource.onComplete()
		}

		private fun isSuitableFileToDownload(childItem: Item): Boolean =
			childItem.audio != null || childItem.image != null || childItem.name.lowercase()
				.endsWith(".txt")
	}

	private inner class DownloadOneDriveFilesTask(
		var mClient: IOneDriveClient,
		var mListener: StorageListener,
		var mItemSource: PublishSubject<DownloadResult>,
		var mMessageSource: PublishSubject<String>,
		var mFilesToDownload: List<FileInfo>,
		var mDownloadFolder: File
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
			this@OneDriveStorage.downloadFiles(mFilesToDownload, mListener, mItemSource, mMessageSource) {
				try {
					val driveFile = mClient.drive.getItems(it.id).buildRequest().get()
					if (driveFile != null) {
						val title = it.name
						Logger.log { "File title: $title" }
						val safeFilename = Utils.makeSafeFilename(title)
						val targetFile = File(mDownloadFolder, safeFilename)
						Logger.log { "Safe filename: $safeFilename" }

						Logger.log("Downloading now ...")
						// Don't check lastModified ... ALWAYS download.
						if (!mListener.shouldCancel()) {
							val localFile = downloadOneDriveFile(mClient, driveFile, targetFile)
							val updatedCloudFile = FileInfo(
								it.id, driveFile.name, driveFile.lastModifiedDateTime.time,
								it.subfolderIds
							)
							SuccessfulDownloadResult(updatedCloudFile, localFile)
						} else
							FailedDownloadResult(it)
					} else
						FailedDownloadResult(it)
				} catch (oneDriveException: OneDriveServiceException) {
					if (oneDriveException.isError(OneDriveErrorCodes.ItemNotFound))
						FailedDownloadResult(it)
					else
						throw oneDriveException
				}
			}

		private fun downloadOneDriveFile(client: IOneDriveClient, file: Item, localFile: File): File =
			localFile.also {
				FileOutputStream(it).use {
					client.drive.getItems(file.id).content.buildRequest().get().use { inStream ->
						Utils.streamToStream(inStream, it)
					}
				}
			}
	}

	private fun doOneDriveAction(action: OneDriveAction) {
		val callback = object : ICallback<IOneDriveClient> {
			override fun success(clientResult: IOneDriveClient) {
				Logger.log("Signed in to OneDrive")
				action.onConnected(clientResult)
			}

			override fun failure(error: ClientException) {
				Logger.log("Failed to sign in to OneDrive")
				action.onAuthenticationRequired()
			}
		}

		val oneDriveConfig = DefaultClientConfig.createWithAuthenticator(oneDriveAuthenticator)
		OneDriveClient.Builder()
			.fromConfig(oneDriveConfig)
			.loginAndBuildClient(parentFragment.requireActivity(), callback)
	}

	private class GetOneDriveRootFolderTask(
		var mClient: IOneDriveClient,
		var mRootPathSource: PublishSubject<FolderInfo>
	) :
		CoroutineTask<Unit, Unit, FolderInfo> {
		override val coroutineContext: CoroutineContext
			get() = Dispatchers.IO

		override fun onPreExecute() {
			// Do nothing.
		}

		override fun onPostExecute(result: FolderInfo) = mRootPathSource.onNext(result)

		override fun onProgressUpdate(progress: Unit) {
			// Do nothing.
		}

		override fun onError(t: Throwable) = mRootPathSource.onError(t)

		override fun doInBackground(params: Unit, progressUpdater: suspend (Unit) -> Unit): FolderInfo =
			mClient.drive.root.buildRequest().get().let {
				FolderInfo(it.id, ONEDRIVE_ROOT_PATH, ONEDRIVE_ROOT_PATH)
			}
	}

	override fun getRootPath(listener: StorageListener, rootPathSource: PublishSubject<FolderInfo>) {
		doOneDriveAction(object : OneDriveAction {
			override fun onConnected(client: IOneDriveClient) {
				GetOneDriveRootFolderTask(client, rootPathSource).execute(Unit)
			}

			override fun onAuthenticationRequired() =
				rootPathSource.onError(StorageException(BeatPrompter.appResources.getString(R.string.could_not_find_cloud_root_error)))
		})
	}

	override fun downloadFiles(
		filesToRefresh: List<FileInfo>,
		storageListener: StorageListener,
		itemSource: PublishSubject<DownloadResult>,
		messageSource: PublishSubject<String>
	) {
		doOneDriveAction(object : OneDriveAction {
			override fun onConnected(client: IOneDriveClient) {
				try {
					DownloadOneDriveFilesTask(
						client,
						storageListener,
						itemSource,
						messageSource,
						filesToRefresh,
						cacheFolder
					).execute(Unit)
				} catch (e: Exception) {
					itemSource.onError(e)
				}
			}

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
		doOneDriveAction(object : OneDriveAction {
			override fun onConnected(client: IOneDriveClient) {
				GetOneDriveFolderContentsTask(
					client,
					this@OneDriveStorage,
					folder,
					listener,
					itemSource,
					messageSource,
					recurseSubFolders
				).execute(Unit)
			}

			override fun onAuthenticationRequired() = listener.onAuthenticationRequired()
		})
	}

	companion object {
		const val ONEDRIVE_CACHE_FOLDER_NAME = "onedrive"
		private const val ONEDRIVE_CLIENT_ID =
			"dc584873-700c-4377-98da-d088cca5c1f5" //This is your client ID
		private const val ONEDRIVE_ROOT_PATH = "/"
	}
}
