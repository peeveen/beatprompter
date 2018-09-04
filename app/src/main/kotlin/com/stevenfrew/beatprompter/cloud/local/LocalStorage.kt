package com.stevenfrew.beatprompter.cloud.local

import android.app.Activity
import android.os.Environment
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cloud.*
import io.reactivex.subjects.PublishSubject
import java.io.File
import java.util.*

class LocalStorage(parentActivity: Activity) : CloudStorage(parentActivity, "local") {
    // No need for region strings here.
    override val cloudStorageName: String
        get() = "local"

    override val directorySeparator: String
        get() = "/"

    // Nobody will ever see this. Any icon will do.
    override val cloudIconResourceId: Int
        get() = R.drawable.ic_device

    override fun getRootPath(listener: CloudListener, rootPathSource: PublishSubject<CloudFolderInfo>) {
        rootPathSource.onNext(CloudFolderInfo(null, Environment.getExternalStorageDirectory().path, "/", "/"))
    }

    override fun downloadFiles(filesToRefresh: List<CloudFileInfo>, cloudListener: CloudListener, itemSource: PublishSubject<CloudDownloadResult>, messageSource: PublishSubject<String>) {
        filesToRefresh.map{SuccessfulCloudDownloadResult(it, File(it.mID))}.forEach {
            messageSource.onNext(BeatPrompterApplication.getResourceString(R.string.downloading, it.cachedCloudFileDescriptor.mName))
            itemSource.onNext(it)
        }
        itemSource.onComplete()
    }

    override fun readFolderContents(folder: CloudFolderInfo, listener: CloudListener, itemSource: PublishSubject<CloudItemInfo>, messageSource: PublishSubject<String>, includeSubfolders: Boolean, returnFolders: Boolean) {
        val localFolder=File(folder.mID)
        messageSource.onNext(BeatPrompterApplication.getResourceString(R.string.scanningFolder, localFolder.name))
        try {
            val files=localFolder.listFiles()
            if(files!=null)
                files.map{if(it.isDirectory) CloudFolderInfo(folder,it.absolutePath,it.name,it.absolutePath) else CloudFileInfo(it.absolutePath,it.name, Date(it.lastModified()),localFolder.name)}.forEach { itemSource.onNext(it) }
            itemSource.onComplete()
        }
        catch(exception:Exception)
        {
            val f=3
            val x=4
        }
    }
}