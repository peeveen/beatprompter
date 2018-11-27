package com.stevenfrew.beatprompter.storage

import com.stevenfrew.beatprompter.util.normalize

abstract class ItemInfo internal constructor(// Unique ID of the item in the storage storage.
        val mID: String, // Display name.
        val mName: String) : Comparable<ItemInfo> {
    val mNormalizedName: String = mName.normalize()

    override operator fun compareTo(other: ItemInfo): Int {
        val thisIsFolder = this is FolderInfo
        val otherIsFolder = other is FolderInfo
        if (thisIsFolder && !otherIsFolder)
            return -1
        return if (!thisIsFolder && otherIsFolder) 1 else mName.toLowerCase().compareTo(other.mName.toLowerCase())
    }
}