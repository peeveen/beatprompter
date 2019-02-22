package com.stevenfrew.beatprompter.storage

import java.util.*

/**
 * A file in a storage system.
 */
open class FileInfo constructor(id: String,
                                name: String,
                                val mLastModified: Date,
                                val mSubfolderIDs: List<String> = listOf(""))
    : ItemInfo(id, name) {

    internal constructor(id: String,
                         name: String,
                         lastModified: Date,
                         subfolderID: String) : this(id, name, lastModified, listOf(subfolderID))
}
