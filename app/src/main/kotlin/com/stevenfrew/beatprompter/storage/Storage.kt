package com.stevenfrew.beatprompter.storage

import android.app.Activity
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.storage.demo.DemoStorage
import com.stevenfrew.beatprompter.storage.dropbox.DropboxStorage
import com.stevenfrew.beatprompter.storage.googledrive.GoogleDriveStorage
import com.stevenfrew.beatprompter.storage.local.LocalStorage
import com.stevenfrew.beatprompter.storage.onedrive.OneDriveStorage
import com.stevenfrew.beatprompter.ui.SongListActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

/**
 * Base class for all storage systems that we will support.
 */
abstract class Storage protected constructor(
	protected var mParentActivity: Activity,
	cloudCacheFolderName: String
) {
	// TODO: Figure out when to call dispose on this.
	private val mCompositeDisposable = CompositeDisposable()

	var cacheFolder: CacheFolder
		protected set

	abstract val cloudStorageName: String

	abstract val directorySeparator: String

	abstract val cloudIconResourceId: Int

	init {
		cacheFolder = CacheFolder(SongListActivity.mBeatPrompterSongFilesFolder!!, cloudCacheFolderName)
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
		for (defaultCloudDownload in SongListActivity.mDefaultDownloads)
			if (refreshFiles.contains(defaultCloudDownload.mFileInfo))
				refreshFiles.remove(defaultCloudDownload.mFileInfo)

		val downloadSource = PublishSubject.create<DownloadResult>()
		mCompositeDisposable.add(
			downloadSource.subscribe(
				{ listener.onItemDownloaded(it) },
				{ listener.onDownloadError(it) },
				{ listener.onDownloadComplete() })
		)
		val messageSource = PublishSubject.create<String>()
		mCompositeDisposable.add(messageSource.subscribe { listener.onProgressMessageReceived(it) })
		// Always include the temporary set list and default midi alias files.
		for (defaultCloudDownload in SongListActivity.mDefaultDownloads)
			downloadSource.onNext(defaultCloudDownload)
		downloadFiles(refreshFiles, listener, downloadSource, messageSource)
	}

	fun readFolderContents(
		folder: FolderInfo,
		listener: FolderSearchListener,
		recurseSubfolders: Boolean
	) {
		val folderContentsSource = PublishSubject.create<ItemInfo>()
		mCompositeDisposable.add(
			folderContentsSource.subscribe(
				{ listener.onCloudItemFound(it) },
				{ listener.onFolderSearchError(it) },
				{ listener.onFolderSearchComplete() })
		)
		val messageSource = PublishSubject.create<String>()
		mCompositeDisposable.add(messageSource.subscribe { listener.onProgressMessageReceived(it) })
		for (defaultCloudDownload in SongListActivity.mDefaultDownloads)
			folderContentsSource.onNext(defaultCloudDownload.mFileInfo)
		readFolderContents(folder, listener, folderContentsSource, messageSource, recurseSubfolders)
	}

	fun selectFolder(parentActivity: Activity, listener: FolderSelectionListener) {
		try {
			getRootPath(object : RootPathListener {
				override fun onRootPathFound(rootPath: FolderInfo) {
					val dialog = ChooseFolderDialog(parentActivity, this@Storage, listener, rootPath)
					dialog.showDialog()
				}

				override fun onRootPathError(t: Throwable) {
					listener.onFolderSelectedError(t)
				}

				override fun onAuthenticationRequired() {
					listener.onAuthenticationRequired()
				}

				override fun shouldCancel(): Boolean {
					return listener.shouldCancel()
				}
			})
		} catch (e: Exception) {
			listener.onFolderSelectedError(e)
		}
	}

	private fun getRootPath(listener: RootPathListener) {
		val rootPathSource = PublishSubject.create<FolderInfo>()
		mCompositeDisposable.add(
			rootPathSource.subscribe(
				{ listener.onRootPathFound(it) },
				{ listener.onRootPathError(it) })
		)
		getRootPath(listener, rootPathSource)
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
		recurseSubfolders: Boolean
	)

	companion object {
		fun getInstance(storageType: StorageType, parentActivity: Activity): Storage {
			return when {
				storageType === StorageType.Dropbox -> DropboxStorage(parentActivity)
				storageType === StorageType.OneDrive -> OneDriveStorage(parentActivity)
				storageType === StorageType.GoogleDrive -> GoogleDriveStorage(parentActivity)
				storageType === StorageType.Local -> LocalStorage(parentActivity)
				else -> DemoStorage(parentActivity)
			}
		}
	}

}