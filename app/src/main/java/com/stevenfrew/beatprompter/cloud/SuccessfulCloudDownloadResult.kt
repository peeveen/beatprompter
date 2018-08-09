package com.stevenfrew.beatprompter.cloud

import java.io.File

class SuccessfulCloudDownloadResult(var cloudFileInfo: CloudFileInfo,var mDownloadedFile:File):CloudDownloadResult(cloudFileInfo)
