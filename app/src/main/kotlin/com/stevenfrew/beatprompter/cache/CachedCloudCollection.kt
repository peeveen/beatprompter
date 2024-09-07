package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.midi.alias.Alias
import com.stevenfrew.beatprompter.storage.FileInfo
import com.stevenfrew.beatprompter.storage.ItemInfo
import com.stevenfrew.beatprompter.util.flattenAll
import com.stevenfrew.beatprompter.util.normalize
import org.w3c.dom.Document
import org.w3c.dom.Element
import kotlin.reflect.full.findAnnotation

/**
 * The file cache.
 */
class CachedCloudCollection {
	private var items = mutableMapOf<String, CachedItem>()

	val folders: List<CachedFolder>
		get() =
			items.values.filterIsInstance<CachedFolder>()

	val songFiles: List<SongFile>
		get() =
			items.values.filterIsInstance<SongFile>()

	val setListFiles: List<SetListFile>
		get() =
			items.values.filterIsInstance<SetListFile>()

	val midiAliasFiles: List<MIDIAliasFile>
		get() =
			items.values.filterIsInstance<MIDIAliasFile>()

	private val audioFiles: List<AudioFile>
		get() =
			items.values.filterIsInstance<AudioFile>()

	private val imageFiles: List<ImageFile>
		get() =
			items.values.filterIsInstance<ImageFile>()

	fun writeToXML(doc: Document, root: Element) =
		items.values.forEach { item ->
			doc.createElement(item::class.findAnnotation<CacheXmlTag>()!!.tag).also {
				item.writeToXML(doc, it)
				root.appendChild(it)
			}
		}

	fun add(cachedItem: CachedItem) {
		items[cachedItem.id] = cachedItem
	}

	fun updateItem(fileInfo: FileInfo) {
		items[fileInfo.id]?.update(fileInfo)
	}

	fun remove(file: ItemInfo) = items.remove(file.id)

	fun compareWithCacheVersion(file: FileInfo): CacheComparisonResult =
		items[file.id]?.let { matchedItem ->
			if (matchedItem is CachedFile && matchedItem.lastModified != file.lastModified)
				CacheComparisonResult.Newer
			else if (file.subfolderIds.size != matchedItem.subfolderIds.size ||
				!file.subfolderIds.containsAll(matchedItem.subfolderIds)
			)
				CacheComparisonResult.Relocated
			else
				CacheComparisonResult.Same
		} ?: CacheComparisonResult.Newer

	fun removeNonExistent(storageIDs: Set<String>) {
		// Delete no-longer-existing files.
		items.values.filterIsInstance<CachedFile>().filterNot { storageIDs.contains(it.id) }
			.forEach { f ->
				if (!f.file.delete())
					Logger.log { "Failed to delete file: ${f.file.name}" }
			}
		// Keep remaining files.
		items = items.filter { storageIDs.contains(it.value.id) }.toMutableMap()
	}

	fun clear() = items.clear()

	private inline fun <reified TCachedFileType : CachedFile> getMappedFiles(
		files: List<CachedFile>,
		filenames: Array<out String>
	): List<TCachedFileType> = filenames.map {
		files.filter { file ->
			file.normalizedName.equals(it.normalize(), ignoreCase = true)
		}
	}
		.flattenAll()
		.filterIsInstance<TCachedFileType>()

	fun getMappedAudioFiles(vararg filenames: String): List<AudioFile> =
		getMappedFiles(audioFiles, filenames)

	fun getMappedImageFiles(vararg filenames: String): List<ImageFile> =
		getMappedFiles(imageFiles, filenames)

	fun getFilesToRefresh(
		fileToRefresh: CachedFile?,
		includeDependencies: Boolean
	): List<CachedFile> =
		mutableListOf<CachedFile>().apply {
			if (fileToRefresh != null) {
				add(fileToRefresh)
				if (fileToRefresh is SongFile && includeDependencies) {
					addAll(fileToRefresh.audioFiles.flatMap {
						it.value.flatMap { audioFile ->
							getMappedAudioFiles(
								audioFile
							)
						}
					})
					addAll(fileToRefresh.imageFiles.flatMap { getMappedImageFiles(it) })
				}
			}
		}

	private fun getParentFolderIDs(subfolderID: String): Set<String> =
		// Get subfolder IDs ...
		((items[subfolderID] as? CachedFolder)?.subfolderIds ?: listOf())
			.toMutableSet().also { resultSet ->
				resultSet.toSet().forEach {
					resultSet.addAll(getParentFolderIDs(it))
				}
			}


	fun isFilterOnly(file: SongFile): Boolean =
		if (file.isFilterOnly)
			true
		else
		// Now ... if any of the folders that the file is in contain a file called ".filter_only"
		// then we treat this file as "filter only". This also applies to any parent folders of
		// those folders.
			file.subfolderIds.toMutableSet().apply {
				toSet().forEach {
					addAll(getParentFolderIDs(it))
				}
			}.let { folderIDs ->
				items.values.any {
					it.name.equals(
						FILTER_ONLY_FILENAME,
						true
					) && it.subfolderIds.count{ id -> id.isNotBlank() } > 0 && it.subfolderIds.intersect(folderIDs).isNotEmpty()
				}
			}

	private fun getSubfolderIDs(folderID: String): Set<String> =
		folders.filter { it.subfolderIds.contains(folderID) }.map { it.id }.toMutableSet().apply {
			toSet().forEach {
				addAll(getSubfolderIDs(it))
			}
		}

	fun getSongsInFolder(folder: CachedFolder): List<SongFile> =
		mutableSetOf(folder.id).apply {
			toSet().forEach {
				addAll(getSubfolderIDs(it))
			}
		}.let { folderIDs ->
			songFiles.filter { it.subfolderIds.intersect(folderIDs).isNotEmpty() }
		}

	internal val midiAliases: List<Alias>
		get() = midiAliasFiles.flatMap { it.aliasSet.aliases }.toList()

	companion object {
		const val FILTER_ONLY_FILENAME = ".filter_only"
	}
}
