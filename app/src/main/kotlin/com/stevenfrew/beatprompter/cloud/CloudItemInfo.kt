package com.stevenfrew.beatprompter.cloud

abstract class CloudItemInfo internal constructor(// Unique ID of the item in the cloud storage.
        var mID: String, // Display name.
        var mName: String):Comparable<CloudItemInfo> {

    override operator fun compareTo(other: CloudItemInfo): Int {
        val thisIsFolder = this is CloudFolderInfo
        val otherIsFolder = other is CloudFolderInfo
        if (thisIsFolder && !otherIsFolder)
            return -1
        return if (!thisIsFolder && otherIsFolder) 1 else mName.toLowerCase().compareTo(other.mName.toLowerCase())
    }
}