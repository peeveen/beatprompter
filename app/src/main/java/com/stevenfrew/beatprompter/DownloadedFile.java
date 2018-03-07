package com.stevenfrew.beatprompter;

import java.io.File;
import java.util.Date;

public class DownloadedFile
{
    File mFile;
    String mID;
    Date mLastModified;
    String mSubfolderName;
    DownloadedFile(File file,String id,Date lastModified,String subfolderName)
    {
        mFile=file;
        mID=id;
        mLastModified=lastModified;
        mSubfolderName=subfolderName;
    }
}
