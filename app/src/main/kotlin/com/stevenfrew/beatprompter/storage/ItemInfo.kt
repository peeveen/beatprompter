package com.stevenfrew.beatprompter.storage

/**
 * Base class for all items stored in a storage system.
 */
abstract class ItemInfo internal constructor(
	val mID: String,
	val mName: String
) : Comparable<ItemInfo> {
	override operator fun compareTo(other: ItemInfo): Int {
		val thisIsFolder = this is FolderInfo
		val otherIsFolder = other is FolderInfo
		if (thisIsFolder && !otherIsFolder)
			return -1
		return if (!thisIsFolder && otherIsFolder) 1
		else mName.lowercase().compareTo(other.mName.lowercase())
	}
}