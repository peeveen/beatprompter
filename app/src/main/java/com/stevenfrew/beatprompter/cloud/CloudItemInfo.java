package com.stevenfrew.beatprompter.cloud;

public abstract class CloudItemInfo {
    // Unique ID of the item in the cloud storage.
    public String mID;
    // Display name.
    public String mName;

    CloudItemInfo(String id,String name)
    {
        mID=id;
        mName=name;
    }

    int compareTo(CloudItemInfo other)
    {
        boolean thisIsFolder=this instanceof CloudFolderInfo;
        boolean otherIsFolder=other instanceof CloudFolderInfo;
        if(thisIsFolder && !otherIsFolder)
            return -1;
        if(!thisIsFolder && otherIsFolder)
            return 1;
        return mName.toLowerCase().compareTo(other.mName.toLowerCase());
    }
}
