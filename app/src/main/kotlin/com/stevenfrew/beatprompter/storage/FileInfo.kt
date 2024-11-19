package com.stevenfrew.beatprompter.storage

import java.util.Date

/**
 * A file in a storage system.
 */
class FileInfo(
	id: String,
	name: String,
	val lastModified: Date,
	val contentHash: String,
	val subfolderIds: List<String> = listOf("")
) : ItemInfo(id, name) {
	internal constructor(
		id: String,
		name: String,
		lastModified: Date,
		contentHash: String,
		subfolderID: String
	) : this(id, name, lastModified, contentHash, listOf(subfolderID))

	override val isFolder: Boolean = false
}
