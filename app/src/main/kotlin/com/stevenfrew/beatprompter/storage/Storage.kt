package com.stevenfrew.beatprompter.storage

import android.app.Activity
import androidx.fragment.app.Fragment
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.storage.demo.DemoStorage
import com.stevenfrew.beatprompter.storage.dropbox.DropboxStorage
import com.stevenfrew.beatprompter.storage.googledrive.GoogleDriveStorage
import com.stevenfrew.beatprompter.storage.local.LocalStorage
import com.stevenfrew.beatprompter.storage.onedrive.OneDriveStorage
import com.stevenfrew.beatprompter.ui.SongListFragment
import com.stevenfrew.beatprompter.util.ProgressReportingListener
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Base class for all storage systems that we will support.
 */
abstract class Storage protected constructor(
	protected var mParentFragment: Fragment,
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
		cacheFolder = CacheFolder(SongListFragment.mBeatPrompterSongFilesFolder!!, cloudCacheFolderName)
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
		for (defaultCloudDownload in SongListFragment.mDefaultDownloads)
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
		mCompositeDisposable.add(messageSource.subscribe {
			reportProgress(listener, it)
		})
		// Always include the temporary set list and default midi alias files.
		for (defaultCloudDownload in SongListFragment.mDefaultDownloads)
			downloadSource.onNext(defaultCloudDownload)
		downloadFiles(refreshFiles, listener, downloadSource, messageSource)
	}

	fun readFolderContents(
		folder: FolderInfo,
		listener: FolderSearchListener,
		recurseSubFolders: Boolean
	) {
		val folderContentsSource = PublishSubject.create<ItemInfo>()
		mCompositeDisposable.add(
			folderContentsSource.subscribe(
				{ listener.onCloudItemFound(it) },
				{ listener.onFolderSearchError(it, mParentFragment.requireContext()) },
				{ listener.onFolderSearchComplete() })
		)
		val messageSource = PublishSubject.create<String>()
		mCompositeDisposable.add(messageSource.subscribe {
			reportProgress(listener, it)
		})
		for (defaultCloudDownload in SongListFragment.mDefaultDownloads)
			folderContentsSource.onNext(defaultCloudDownload.mFileInfo)
		readFolderContents(folder, listener, folderContentsSource, messageSource, recurseSubFolders)
	}

	fun selectFolder(parentActivity: Activity, listener: FolderSelectionListener) {
		try {
			getRootPath(object : RootPathListener {
				override fun onRootPathFound(rootPath: FolderInfo) {
					val dialog = ChooseFolderDialog(parentActivity, this@Storage, listener, rootPath)
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
		recurseSubFolders: Boolean
	)

	companion object {
		private fun reportProgress(listener: ProgressReportingListener<String>, message: String) {
			runBlocking {
				launch {
					listener.onProgressMessageReceived(message)
				}
			}
		}

		fun getInstance(storageType: StorageType, parentFragment: Fragment): Storage {
			return when {
				storageType === StorageType.Dropbox -> DropboxStorage(parentFragment)
				storageType === StorageType.OneDrive -> OneDriveStorage(parentFragment)
				storageType === StorageType.GoogleDrive -> GoogleDriveStorage(parentFragment)
				storageType === StorageType.Local -> LocalStorage(parentFragment)
				else -> DemoStorage(parentFragment)
			}
		}
	}

}