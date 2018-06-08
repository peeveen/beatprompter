package com.stevenfrew.beatprompter.cloud;

import java.io.File;

public class CloudDownloadResult {
    public File mDownloadedFile;
    public CloudFileInfo mCloudFileInfo;
    public CloudDownloadResultType mResultType;

    public CloudDownloadResult(CloudFileInfo cloudFileInfo,File downloadedFile)
    {
        this(cloudFileInfo,CloudDownloadResultType.Succeeded);
        mDownloadedFile=downloadedFile;
    }

    public CloudDownloadResult(CloudFileInfo cloudFileInfo,CloudDownloadResultType resultType)
    {
        mCloudFileInfo=cloudFileInfo;
        mResultType=resultType;
    }
}
