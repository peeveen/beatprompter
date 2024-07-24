package com.stevenfrew.beatprompter.storage

/**
 * Base class for all items stored in a storage system.
 */
abstract class ItemInfo internal constructor(
	val id: String,
	val name: String
) : Comparable<ItemInfo> {
	override operator fun compareTo(other: ItemInfo): Int {
		val thisIsFolder = isFolder
		val otherIsFolder = other.isFolder
		return if (thisIsFolder && !otherIsFolder)
			-1
		else if (!thisIsFolder && otherIsFolder)
			1
		else
			name.lowercase().compareTo(other.name.lowercase())
	}

	abstract val isFolder: Boolean
}