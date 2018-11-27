package com.stevenfrew.beatprompter.storage

class FolderInfo// The ID field of these objects will be whatever the underlying storage system uses for listing folder contents.
// It might be an actual number/hex ID, or it might be a path string.
(// The parent item, used for navigating upwards in the folder browser.
        var mParentFolder: FolderInfo?, id: String, name: String, // Path for displaying this in the preferences.
        var mDisplayPath: String) : ItemInfo(id, name) {

    constructor(id: String, folderDisplayName: String, displayPath: String) : this(null, id, folderDisplayName, displayPath)

    internal constructor(rootFolderIdentifier: String) : this(null, rootFolderIdentifier, rootFolderIdentifier, rootFolderIdentifier)
}