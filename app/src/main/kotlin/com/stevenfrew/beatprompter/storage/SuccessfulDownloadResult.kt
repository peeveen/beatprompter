package com.stevenfrew.beatprompter.storage

import com.stevenfrew.beatprompter.cache.CachedFileDescriptor
import java.io.File

/**
 * Represents a successful download from the storage system.
 * Contains the local file that was downloaded.
 */
class SuccessfulDownloadResult(fileInfo: FileInfo, var mDownloadedFile: File) : DownloadResult(fileInfo) {
    val cachedCloudFileDescriptor get() = CachedFileDescriptor(mDownloadedFile, mFileInfo)
}
