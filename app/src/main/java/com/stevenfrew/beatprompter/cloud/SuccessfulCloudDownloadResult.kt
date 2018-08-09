package com.stevenfrew.beatprompter.cloud

import java.io.File

class SuccessfulCloudDownloadResult(cloudFileInfo: CloudFileInfo,var mDownloadedFile:File):CloudDownloadResult(cloudFileInfo)
