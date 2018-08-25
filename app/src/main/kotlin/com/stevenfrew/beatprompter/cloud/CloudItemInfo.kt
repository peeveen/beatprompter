package com.stevenfrew.beatprompter.cloud

import com.stevenfrew.beatprompter.Utils

abstract class CloudItemInfo internal constructor(// Unique ID of the item in the cloud storage.
        val mID: String, // Display name.
        val mName: String):Comparable<CloudItemInfo> {
    val mNormalizedName:String

    init {
        mNormalizedName= Utils.normalizeString(mName)
    }

    override operator fun compareTo(other: CloudItemInfo): Int {
        val thisIsFolder = this is CloudFolderInfo
        val otherIsFolder = other is CloudFolderInfo
        if (thisIsFolder && !otherIsFolder)
            return -1
        return if (!thisIsFolder && otherIsFolder) 1 else mName.toLowerCase().compareTo(other.mName.toLowerCase())
    }
}