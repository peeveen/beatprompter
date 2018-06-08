package com.stevenfrew.beatprompter.cloud;

public class CloudBrowserItem {
    String mDisplayName;
    public String mInternalPath;
    boolean mIsFolder;
    CloudBrowserItem mParentFolder;
    public CloudBrowserItem(String displayName, String internalPath, boolean isFolder)
    {
        mParentFolder=null;
        mDisplayName=displayName;
        mInternalPath=internalPath;
        mIsFolder=isFolder;
    }
    public CloudBrowserItem(CloudBrowserItem parent, String displayName, String internalPath, boolean isFolder)
    {
        this(displayName,internalPath,isFolder);
        mParentFolder=parent;
    }
    public String toString()
    {
        return mDisplayName;
    }
    int compareTo(CloudBrowserItem other)
    {
        if((mIsFolder)&&(!other.mIsFolder))
            return -1;
        if((!mIsFolder)&&(other.mIsFolder))
            return 1;
        return mDisplayName.toLowerCase().compareTo(other.mDisplayName.toLowerCase());
    }
}
