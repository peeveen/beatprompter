package com.stevenfrew.beatprompter.cloud;

public class CloudFolderInfo extends CloudItemInfo {
    // Path for displaying this in the preferences.
    public String mDisplayPath;
    // The parent item, used for navigating upwards in the folder browser.
    public CloudFolderInfo mParentFolder;

    // The ID field of these objects will be whatever the underlying cloud system uses for listing folder contents.
    // It might be an actual number/hex ID, or it might be a path string.
    public CloudFolderInfo(CloudFolderInfo parentFolder,String id,String name,String displayPath)
    {
        super(id,name);
        mDisplayPath=displayPath;
        mParentFolder=parentFolder;
    }

    public CloudFolderInfo(String id,String folderDisplayName,String displayPath)
    {
        this(null,id,folderDisplayName,displayPath);
    }

    CloudFolderInfo(String rootFolderIdentifier)
    {
        this(null,rootFolderIdentifier,rootFolderIdentifier,rootFolderIdentifier);
    }
}
