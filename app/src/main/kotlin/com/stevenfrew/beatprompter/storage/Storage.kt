package com.stevenfrew.beatprompter.storage

import android.app.Activity
import androidx.fragment.app.Fragment
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.cache.Cache
import com.stevenfrew.beatprompter.storage.demo.DemoStorage
import com.stevenfrew.beatprompter.storage.dropbox.DropboxStorage
import com.stevenfrew.beatprompter.storage.googledrive.GoogleDriveStorage
import com.stevenfrew.beatprompter.storage.local.LocalStorage
import com.stevenfrew.beatprompter.storage.onedrive.OneDriveStorage
import com.stevenfrew.beatprompter.util.Utils
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

/**
 * Base class for all storage systems that we will support.
 */
abstract class Storage protected constructor(
	protected var mParentFragment: Fragment,
	storageType: StorageType
) {
	var cacheFolder: CacheFolder
		protected set

	abstract val cloudStorageName: String

	abstract val directorySeparator: String

	abstract val cloudIconResourceId: Int

	init {
		cacheFolder = Cache.getCacheFolderForStorage(storageType)
		if (!cacheFolder.exists())
			if (!cacheFolder.mkdir())
				Logger.log("Failed to create storage cache folder.")
	}

	fun constructFullPath(folderPath: String, itemName: String): String {
		var fullPath = folderPath
		if (!fullPath.endsWith(directorySeparator))
			fullPath += directorySeparator
		return fullPath + itemName
	}

	fun downloadFiles(filesToRefresh: List<FileInfo>, listener: ItemDownloadListener) {
		val refreshFiles = filesToRefresh.toMutableList()
		Cache.mDefaultDownloads
			.map { it.mFileInfo }
			.filter { refreshFiles.contains(it) }
			.forEach { refreshFiles.remove(it) }

		val downloadSource = PublishSubject.create<DownloadResult>()
		val messageSource = PublishSubject.create<String>()
		CompositeDisposable().apply {
			add(
				downloadSource.subscribe(
					{ listener.onItemDownloaded(it) },
					{ listener.onDownloadError(it) },
					{
						listener.onDownloadComplete()
						this.dispose()
					})
			)
			add(messageSource.subscribe {
				Utils.reportProgress(listener, it)
			})
		}
		// Always include the temporary set list and default midi alias files.
		Cache.mDefaultDownloads.forEach { downloadSource.onNext(it) }
		downloadFiles(refreshFiles, listener, downloadSource, messageSource)
	}

	fun readFolderContents(
		folder: FolderInfo,
		listener: FolderSearchListener,
		recurseSubFolders: Boolean
	) {
		val folderContentsSource = PublishSubject.create<ItemInfo>()
		val messageSource = PublishSubject.create<String>()
		CompositeDisposable().apply {
			add(
				folderContentsSource.subscribe(
					{ listener.onCloudItemFound(it) },
					{ listener.onFolderSearchError(it, mParentFragment.requireContext()) },
					{
						listener.onFolderSearchComplete()
					})
			)
			add(messageSource.subscribe {
				Utils.reportProgress(listener, it)
			})
		}
		for (defaultCloudDownload in Cache.mDefaultDownloads)
			folderContentsSource.onNext(defaultCloudDownload.mFileInfo)
		readFolderContents(folder, listener, folderContentsSource, messageSource, recurseSubFolders)
	}

	fun selectFolder(parentActivity: Activity, listener: FolderSelectionListener) {
		try {
			getRootPath(object : RootPathListener {
				override fun onRootPathFound(rootPath: FolderInfo) {
					val dialog =
						ChooseFolderDialog(parentActivity, this@Storage, listener, rootPath, parentActivity)
					dialog.showDialog()
				}

				override fun onRootPathError(t: Throwable) {
					listener.onFolderSelectedError(t, mParentFragment.requireContext())
				}

				override fun onAuthenticationRequired() {
					listener.onAuthenticationRequired()
				}

				override fun shouldCancel(): Boolean {
					return listener.shouldCancel()
				}
			})
		} catch (e: Exception) {
			listener.onFolderSelectedError(e, mParentFragment.requireContext())
		}
	}

	private fun getRootPath(listener: RootPathListener) {
		val rootPathSource = PublishSubject.create<FolderInfo>()
		val subscription = rootPathSource.subscribe(
			{ listener.onRootPathFound(it) },
			{ listener.onRootPathError(it) }
		)
		getRootPath(listener, rootPathSource)
		subscription.dispose()
	}

	protected abstract fun getRootPath(
		listener: StorageListener,
		rootPathSource: PublishSubject<FolderInfo>
	)

	protected abstract fun downloadFiles(
		filesToRefresh: List<FileInfo>,
		storageListener: StorageListener,
		itemSource: PublishSubject<DownloadResult>,
		messageSource: PublishSubject<String>
	)

	protected abstract fun readFolderContents(
		folder: FolderInfo,
		listener: StorageListener,
		itemSource: PublishSubject<ItemInfo>,
		messageSource: PublishSubject<String>,
		recurseSubFolders: Boolean
	)

	companion object {
		fun getInstance(storageType: StorageType, parentFragment: Fragment): Storage {
			return when {
				storageType === StorageType.Dropbox -> DropboxStorage(parentFragment)
				storageType === StorageType.OneDrive -> OneDriveStorage(parentFragment)
				storageType === StorageType.GoogleDrive -> GoogleDriveStorage(parentFragment)
				storageType === StorageType.Local -> LocalStorage(parentFragment)
				else -> DemoStorage(parentFragment)
			}
		}

		fun getCacheFolderName(storageType: StorageType): String {
			return when {
				storageType === StorageType.Dropbox -> DropboxStorage.DROPBOX_CACHE_FOLDER_NAME
				storageType === StorageType.OneDrive -> OneDriveStorage.ONEDRIVE_CACHE_FOLDER_NAME
				storageType === StorageType.GoogleDrive -> GoogleDriveStorage.GOOGLE_DRIVE_CACHE_FOLDER_NAME
				storageType === StorageType.Local -> LocalStorage.LOCAL_CACHE_FOLDER_NAME
				else -> DemoStorage.DEMO_CACHE_FOLDER_NAME
			}
		}
	}

}