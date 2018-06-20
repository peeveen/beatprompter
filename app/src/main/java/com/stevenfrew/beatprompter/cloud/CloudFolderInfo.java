package com.stevenfrew.beatprompter.cloud;

public class CloudFolderInfo extends CloudItemInfo {
    // Path to this item for storing in the preferences.
    public String mInternalPath;
    // The parent item, used for navigating upwards in the folder browser.
    public CloudFolderInfo mParentFolder;

    public CloudFolderInfo(CloudFolderInfo parentFolder,String id,String folderDisplayName,String internalPath)
    {
        super(id,folderDisplayName);
        mParentFolder=parentFolder;
        mInternalPath=internalPath;
    }

    public CloudFolderInfo(String id,String folderDisplayName,String internalPath)
    {
        this(null,id,folderDisplayName,internalPath);
    }

    public CloudFolderInfo(String rootFolderIdentifier)
    {
        this(null,rootFolderIdentifier,rootFolderIdentifier,rootFolderIdentifier);
    }
}
