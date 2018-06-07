package com.stevenfrew.beatprompter;

class CloudBrowserItem {
    String mDisplayName;
    String mInternalPath;
    boolean mIsFolder;
    CloudBrowserItem mParentFolder;
    CloudBrowserItem(String displayName, String internalPath, boolean isFolder)
    {
        mParentFolder=null;
        mDisplayName=displayName;
        mInternalPath=internalPath;
        mIsFolder=isFolder;
    }
    CloudBrowserItem(CloudBrowserItem parent, String displayName, String internalPath, boolean isFolder)
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
