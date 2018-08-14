package com.stevenfrew.beatprompter.cloud

class CloudFolderInfo// The ID field of these objects will be whatever the underlying cloud system uses for listing folder contents.
// It might be an actual number/hex ID, or it might be a path string.
(// The parent item, used for navigating upwards in the folder browser.
        var mParentFolder: CloudFolderInfo?, id: String, name: String, // Path for displaying this in the preferences.
        var mDisplayPath: String) : CloudItemInfo(id, name) {

    constructor(id: String, folderDisplayName: String, displayPath: String) : this(null, id, folderDisplayName, displayPath)

    internal constructor(rootFolderIdentifier: String) : this(null, rootFolderIdentifier, rootFolderIdentifier, rootFolderIdentifier)
}