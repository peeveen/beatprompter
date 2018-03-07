package com.stevenfrew.beatprompter;

class CloudItem {
    String mDisplayName;
    String mInternalPath;
    boolean mIsFolder;
    CloudItem mParentFolder;
    CloudItem(String displayName,String internalPath,boolean isFolder)
    {
        mParentFolder=null;
        mDisplayName=displayName;
        mInternalPath=internalPath;
        mIsFolder=isFolder;
    }
    CloudItem(CloudItem parent,String displayName,String internalPath,boolean isFolder)
    {
        this(displayName,internalPath,isFolder);
        mParentFolder=parent;
    }
    public String toString()
    {
        return mDisplayName;
    }
    int compareTo(CloudItem other)
    {
        if((mIsFolder)&&(!other.mIsFolder))
            return -1;
        if((!mIsFolder)&&(other.mIsFolder))
            return 1;
        return mDisplayName.toLowerCase().compareTo(other.mDisplayName.toLowerCase());
    }
}
