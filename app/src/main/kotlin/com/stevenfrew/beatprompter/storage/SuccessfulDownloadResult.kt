package com.stevenfrew.beatprompter.storage

import com.stevenfrew.beatprompter.cache.CachedFile
import java.io.File

/**
 * Represents a successful download from the storage system.
 * Contains the local file that was downloaded.
 */
class SuccessfulDownloadResult(fileInfo: FileInfo,
                               private val mDownloadedFile: File)
    : DownloadResult(fileInfo) {
    val cachedCloudFileDescriptor
        get() =
            CachedFile(mDownloadedFile,
                    mFileInfo.mID,
                    mFileInfo.mName,
                    mFileInfo.mLastModified,
                    mFileInfo.mSubfolderIDs)
}
