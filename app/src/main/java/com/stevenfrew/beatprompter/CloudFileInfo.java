package com.stevenfrew.beatprompter;

import java.util.Date;

class CloudFileInfo {
    String mStorageID;
    String mTitle;
    Date mLastModified;
    String mSubfolder;

    CloudFileInfo(String storageID,String title,Date lastModified,String subfolder)
    {
        mStorageID=storageID;
        mTitle=title;
        mLastModified=lastModified;
        mSubfolder=subfolder;
    }
}
