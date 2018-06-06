package com.stevenfrew.beatprompter;

import java.io.File;

public class DownloadedFile
{
    CloudFileInfo mCloudFileInfo;
    File mFile;

    DownloadedFile(CloudDownloadResult result)
    {
        mCloudFileInfo=result.mCloudFileInfo;
        mFile=result.mDownloadedFile;
    }
}
