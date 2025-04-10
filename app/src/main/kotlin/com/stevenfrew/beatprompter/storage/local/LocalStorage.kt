package com.stevenfrew.beatprompter.storage.local

import android.os.Environment
import androidx.fragment.app.Fragment
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.storage.DownloadResult
import com.stevenfrew.beatprompter.storage.FileInfo
import com.stevenfrew.beatprompter.storage.FolderInfo
import com.stevenfrew.beatprompter.storage.ItemInfo
import com.stevenfrew.beatprompter.storage.Storage
import com.stevenfrew.beatprompter.storage.StorageListener
import com.stevenfrew.beatprompter.storage.StorageType
import com.stevenfrew.beatprompter.storage.SuccessfulDownloadResult
import com.stevenfrew.beatprompter.util.getMd5Hash
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.Date

/**
 * Local storage implementation of the storage system.
 */
class LocalStorage(parentFragment: Fragment) : Storage(parentFragment, StorageType.Local) {
	// No need for region strings here.
	override val cloudStorageName: String
		get() = "local"

	override val directorySeparator: String
		get() = "/"

	override val cloudIconResourceId: Int
		get() = R.drawable.ic_device

	override fun getRootPath(listener: StorageListener, rootPathSource: PublishSubject<FolderInfo>) {
		// Android storage APIs are notoriously stupid, dating from a time when SD cards in phones
		// were not a concept that anyone really considered. So when we ask for the "external"
		// storage directory, it returns the path of the internal memory filesystem.
		val internalStoragePath = Environment.getExternalStorageDirectory().absolutePath
		// This returns ALL the folders that our app has access to. If there are multiple
		// storage locations (e.g. internal memory AND an SD card), then it will return more than
		// one. The internal memory is always first, so we'll use the last one to get an
		// actual EXTERNAL storage path. Sadly it will be suffixed with "/android/our_app_id/blah/blah",
		// but we can do something about that.
		val folderPath = parentFragment.requireContext().getExternalFilesDirs(null).last().absolutePath
		// If the folderPath starts with "/storage/emulated/0" (the internal storage path), use the internal storage path.
		// But if it doesn't, then there's an SD card in play, so use that. The SD card path will start with "/storage/" then
		// the ID of the card, so we can adjust the substring for that.
		val pathToUse =
			if (folderPath.startsWith(internalStoragePath)) internalStoragePath else folderPath.substring(
				0,
				folderPath.indexOf('/', 9)
			)
		rootPathSource.onNext(FolderInfo(null, pathToUse, "/", "/"))
	}

	override fun downloadFiles(
		filesToRefresh: List<FileInfo>,
		storageListener: StorageListener,
		itemSource: PublishSubject<DownloadResult>,
		messageSource: PublishSubject<String>
	) = downloadFiles(filesToRefresh, storageListener, itemSource, messageSource) { cloudFile ->
		File(cloudFile.id).let {
			SuccessfulDownloadResult(
				FileInfo(
					it.absolutePath,
					it.name,
					Date(it.lastModified()),
					it.getMd5Hash(),
					cloudFile.subfolderIds
				), it
			)
		}
	}

	override fun readFolderContents(
		folder: FolderInfo,
		listener: StorageListener,
		itemSource: PublishSubject<ItemInfo>,
		messageSource: PublishSubject<String>,
		recurseSubFolders: Boolean
	) {
		val foldersToSearch = mutableListOf(folder)
		while (foldersToSearch.isNotEmpty()) {
			val folderToSearch = foldersToSearch.removeAt(0)
			val localFolder = File(folderToSearch.id)
			messageSource.onNext(
				BeatPrompter.appResources.getString(
					R.string.scanningFolder,
					localFolder.name
				)
			)
			try {
				val files = localFolder.listFiles()
				if (files != null) {
					files.filter { it.isFile }
						.map {
							FileInfo(
								it.absolutePath,
								it.name,
								Date(it.lastModified()),
								it.getMd5Hash(),
								it.absolutePath
							)
						}
						.forEach { itemSource.onNext(it) }
					if (recurseSubFolders)
						foldersToSearch.addAll(files.filter { it.isDirectory }
							.map { FolderInfo(folderToSearch, it.absolutePath, it.name, it.absolutePath) })
					files.filter { it.isDirectory }
						.map { FolderInfo(folderToSearch, it.absolutePath, it.name, it.absolutePath) }
						.forEach { itemSource.onNext(it) }
				}
			} catch (e: Exception) {
				itemSource.onError(e)
				return
			}
		}
		itemSource.onComplete()
	}

	companion object {
		const val LOCAL_CACHE_FOLDER_NAME = "local"
	}
}