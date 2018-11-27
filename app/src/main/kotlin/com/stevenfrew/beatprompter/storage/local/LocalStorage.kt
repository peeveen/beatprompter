package com.stevenfrew.beatprompter.storage.local

import android.app.Activity
import android.os.Environment
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.storage.*
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.*

class LocalStorage(parentActivity: Activity) : Storage(parentActivity, "local") {
    // No need for region strings here.
    override val cloudStorageName: String
        get() = "local"

    override val directorySeparator: String
        get() = "/"

    override val cloudIconResourceId: Int
        get() = R.drawable.ic_device

    override fun getRootPath(listener: StorageListener, rootPathSource: PublishSubject<FolderInfo>) {
        rootPathSource.onNext(FolderInfo(null, Environment.getExternalStorageDirectory().path, "/", "/"))
    }

    override fun downloadFiles(filesToRefresh: List<FileInfo>, storageListener: StorageListener, itemSource: PublishSubject<DownloadResult>, messageSource: PublishSubject<String>) {
        filesToRefresh.map { sourceCloudFile -> File(sourceCloudFile.mID) to sourceCloudFile.mSubfolder }.map { updatedFile -> SuccessfulDownloadResult(FileInfo(updatedFile.first.absolutePath, updatedFile.first.name, Date(updatedFile.first.lastModified()), updatedFile.second), updatedFile.first) }.forEach {
            messageSource.onNext(BeatPrompterApplication.getResourceString(R.string.downloading, it.cachedCloudFileDescriptor.mName))
            itemSource.onNext(it)
        }
        itemSource.onComplete()
    }

    override fun readFolderContents(folder: FolderInfo, listener: StorageListener, itemSource: PublishSubject<ItemInfo>, messageSource: PublishSubject<String>, includeSubfolders: Boolean, returnFolders: Boolean) {
        val foldersToSearch = mutableListOf(folder)
        while (foldersToSearch.isNotEmpty()) {
            val folderToSearch = foldersToSearch.removeAt(0)
            val localFolder = File(folderToSearch.mID)
            messageSource.onNext(BeatPrompterApplication.getResourceString(R.string.scanningFolder, localFolder.name))
            try {
                val files = localFolder.listFiles()
                if (files != null) {
                    files.filter { it.isFile }.map { FileInfo(it.absolutePath, it.name, Date(it.lastModified()), localFolder.name) }.forEach { itemSource.onNext(it) }
                    if (includeSubfolders)
                        foldersToSearch.addAll(files.filter { it.isDirectory }.map { FolderInfo(folderToSearch, it.absolutePath, it.name, it.absolutePath) })
                    if (returnFolders)
                        files.filter { it.isDirectory }.map { FolderInfo(folderToSearch, it.absolutePath, it.name, it.absolutePath) }.forEach { itemSource.onNext(it) }
                }
            } catch (e: Exception) {
                itemSource.onError(e)
                return
            }
        }
        itemSource.onComplete()
    }
}