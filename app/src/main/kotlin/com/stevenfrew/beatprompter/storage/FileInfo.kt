package com.stevenfrew.beatprompter.storage

import java.util.*

/**
 * A file in a storage system.
 */
open class FileInfo(id: String,
                    name: String,
                    val mLastModified: Date,
                    val mSubfolderIDs: List<String> = listOf())
    : ItemInfo(id, name)
