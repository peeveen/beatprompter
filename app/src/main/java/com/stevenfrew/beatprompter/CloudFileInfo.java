package com.stevenfrew.beatprompter;

import java.util.Date;

class CloudFileInfo {
    String mStorageID;
    String mName;
    Date mLastModified;
    String mSubfolder;

    CloudFileInfo(String storageID,String name,Date lastModified,String subfolder)
    {
        mStorageID=storageID;
        mName=name;
        mLastModified=lastModified;
        mSubfolder=subfolder;
    }
}
