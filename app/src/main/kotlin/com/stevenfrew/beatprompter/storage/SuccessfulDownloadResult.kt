package com.stevenfrew.beatprompter.storage

import com.stevenfrew.beatprompter.cache.CachedFileDescriptor
import java.io.File

class SuccessfulDownloadResult(fileInfo: FileInfo, var mDownloadedFile: File) : DownloadResult(fileInfo) {
    val cachedCloudFileDescriptor get() = CachedFileDescriptor(mDownloadedFile, mFileInfo)
}
