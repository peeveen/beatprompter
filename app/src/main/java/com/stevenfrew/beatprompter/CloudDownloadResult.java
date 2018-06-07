package com.stevenfrew.beatprompter;

import java.io.File;
import java.util.Date;

class CloudDownloadResult {
    File mDownloadedFile;
    CloudFileInfo mCloudFileInfo;
    CloudDownloadResultType mResultType;

    CloudDownloadResult(CloudFileInfo cloudFileInfo,File downloadedFile)
    {
        this(cloudFileInfo,CloudDownloadResultType.Succeeded);
        mDownloadedFile=downloadedFile;
    }

    CloudDownloadResult(CloudFileInfo cloudFileInfo,CloudDownloadResultType resultType)
    {
        mCloudFileInfo=cloudFileInfo;
        mResultType=resultType;
    }
}
