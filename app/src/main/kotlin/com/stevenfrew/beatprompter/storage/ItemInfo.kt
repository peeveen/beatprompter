package com.stevenfrew.beatprompter.storage

import com.stevenfrew.beatprompter.util.normalize

/**
 * Base class for all items stored in a storage system.
 */
abstract class ItemInfo internal constructor(val mID: String,
                                             val mName: String)
    : Comparable<ItemInfo> {
    val mNormalizedName: String = mName.normalize()

    override operator fun compareTo(other: ItemInfo): Int {
        val thisIsFolder = this is FolderInfo
        val otherIsFolder = other is FolderInfo
        if (thisIsFolder && !otherIsFolder)
            return -1
        return if (!thisIsFolder && otherIsFolder) 1
        else mName.toLowerCase().compareTo(other.mName.toLowerCase())
    }
}