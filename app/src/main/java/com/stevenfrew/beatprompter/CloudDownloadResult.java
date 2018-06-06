package com.stevenfrew.beatprompter;

import java.io.File;
import java.util.Date;

class CloudDownloadResult {
    String mStorageID;
    File mDownloadedFile;
    CloudFileInfo mCloudFileInfo;
    CloudDownloadResultType mResultType;
    Exception mException;

    CloudDownloadResult(CloudFileInfo cloudFileInfo,File downloadedFile)
    {
        this(cloudFileInfo,CloudDownloadResultType.Succeeded);
        mDownloadedFile=downloadedFile;
    }

    CloudDownloadResult(CloudFileInfo cloudFileInfo,CloudDownloadResultType resultType)
    {
        mStorageID=cloudFileInfo.mStorageID;
        mCloudFileInfo=cloudFileInfo;
        mResultType=resultType;
    }

    CloudDownloadResult(String storageID,Exception e)
    {
        mStorageID=storageID;
        mResultType=CloudDownloadResultType.Failed;
        mException=e;
    }
}
