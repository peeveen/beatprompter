package com.stevenfrew.beatprompter.storage

import com.stevenfrew.beatprompter.cache.CachedFile
import java.io.File

/**
 * Represents a successful download from the storage system.
 * Contains the local file that was downloaded.
 */
class SuccessfulDownloadResult(
	fileInfo: FileInfo,
	private val downloadedFile: File
) : DownloadResult(fileInfo) {
	val cachedCloudFile
		get() =
			CachedFile(
				downloadedFile,
				fileInfo.id,
				fileInfo.name,
				fileInfo.lastModified,
				fileInfo.subfolderIds
			)
}
