package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.AudioFileParser
import com.stevenfrew.beatprompter.cache.parse.ImageFileParser
import com.stevenfrew.beatprompter.cache.parse.InvalidBeatPrompterFileException
import com.stevenfrew.beatprompter.cache.parse.MidiAliasFileParser
import com.stevenfrew.beatprompter.cache.parse.SetListFileParser
import com.stevenfrew.beatprompter.cache.parse.SongInfoParser
import com.stevenfrew.beatprompter.cache.parse.TextContentProvider
import com.stevenfrew.beatprompter.storage.SuccessfulDownloadResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.util.Date

/**
 * A description of a cached storage file. Basically a file on the filesystem, and relevant info about
 * it's source.
 */
open class CachedFile : CachedItem, TextContentProvider {
	val file: File
	val lastModified: Date
	val contentHash: String

	constructor(
		file: File,
		id: String,
		name: String,
		lastModified: Date,
		contentHash: String,
		subfolderIDs: List<String>
	) : super(id, name, subfolderIDs) {
		this.file = file
		this.contentHash = contentHash
		this.lastModified = lastModified
	}

	constructor(cachedFile: CachedFile) : this(
		cachedFile.file,
		cachedFile.id,
		cachedFile.name,
		cachedFile.lastModified,
		cachedFile.contentHash,
		cachedFile.subfolderIds
	)

	constructor(element: Element) : super(element) {
		lastModified = Date(element.getAttribute(CACHED_FILE_LAST_MODIFIED_ATTRIBUTE_NAME).toLong())
		file = File(element.getAttribute(CACHED_FILE_PATH_ATTRIBUTE_NAME))
		contentHash = element.getAttribute(CONTENT_HASH_ATTRIBUTE_NAME)
	}

	override fun writeToXML(doc: Document, element: Element) {
		super.writeToXML(doc, element)
		element.setAttribute(CACHED_FILE_PATH_ATTRIBUTE_NAME, file.absolutePath)
		element.setAttribute(CACHED_FILE_LAST_MODIFIED_ATTRIBUTE_NAME, "${lastModified.time}")
		element.setAttribute(CONTENT_HASH_ATTRIBUTE_NAME, contentHash)
	}

	companion object {
		private const val CACHED_FILE_PATH_ATTRIBUTE_NAME = "path"
		private const val CACHED_FILE_LAST_MODIFIED_ATTRIBUTE_NAME = "lastModified"
		private const val CONTENT_HASH_ATTRIBUTE_NAME = "contentHash"

		fun createCachedCloudFile(result: SuccessfulDownloadResult): CachedFile =
			try {
				AudioFileParser(result.cachedCloudFile).parse()
			} catch (_: InvalidBeatPrompterFileException) {
				try {
					ImageFileParser(result.cachedCloudFile).parse()
				} catch (_: InvalidBeatPrompterFileException) {
					try {
						MidiAliasFileParser(result.cachedCloudFile).parse()
					} catch (_: InvalidBeatPrompterFileException) {
						try {
							SongInfoParser(result.cachedCloudFile).parse()
						} catch (_: InvalidBeatPrompterFileException) {
							try {
								SetListFileParser(result.cachedCloudFile).parse()
							} catch (_: InvalidBeatPrompterFileException) {
								IrrelevantFile(result.cachedCloudFile)
							}
						}
					}
				}
			}
	}

	override fun getContent(): String = if (file.exists()) file.readText() else ""
}