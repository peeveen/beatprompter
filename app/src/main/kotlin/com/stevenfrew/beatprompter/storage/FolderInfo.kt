package com.stevenfrew.beatprompter.storage

/**
 * A folder in a storage system.
 * The ID of these objects will be whatever the underlying storage system uses for listing folder contents.
 * It might be an actual number/hex ID, or it might be a path string.
 * [mDisplayPath] is the path for displaying this in the preferences.
 */
class FolderInfo(val mParentFolder: FolderInfo?,
                 id: String,
                 name: String,
                 val mDisplayPath: String,
                 val mFilterOnly: Boolean)
    : ItemInfo(id, name) {
    constructor(id: String, folderDisplayName: String, displayPath: String, filterOnly: Boolean)
            : this(null, id, folderDisplayName, displayPath, filterOnly)

    internal constructor(rootFolderIdentifier: String)
            : this(null, rootFolderIdentifier, rootFolderIdentifier, rootFolderIdentifier, false)
}