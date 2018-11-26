package com.stevenfrew.beatprompter.cloud

import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor
import java.io.File

class SuccessfulCloudDownloadResult(cloudFileInfo: CloudFileInfo, var mDownloadedFile: File) : CloudDownloadResult(cloudFileInfo) {
    val cachedCloudFileDescriptor get() = CachedCloudFileDescriptor(mDownloadedFile, mCloudFileInfo)
}
