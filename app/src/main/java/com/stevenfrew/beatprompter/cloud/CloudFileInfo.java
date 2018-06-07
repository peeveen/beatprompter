package com.stevenfrew.beatprompter.cloud;

import java.util.Date;

public class CloudFileInfo
{
    public String mStorageID;
    public String mName;
    public Date mLastModified;
    public String mSubfolder;

    public CloudFileInfo(String storageID,String name,Date lastModified,String subfolder)
    {
        mStorageID=storageID;
        mName=name;
        mLastModified=lastModified;
        mSubfolder=subfolder;
    }
}
