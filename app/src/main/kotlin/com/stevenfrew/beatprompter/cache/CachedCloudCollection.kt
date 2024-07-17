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
	private var mItems = mutableMapOf<String, CachedItem>()

	val folders: List<CachedFolder>
		get() =
			mItems.values.filterIsInstance<CachedFolder>()

	val songFiles: List<SongFile>
		get() =
			mItems.values.filterIsInstance<SongFile>()

	val setListFiles: List<SetListFile>
		get() =
			mItems.values.filterIsInstance<SetListFile>()

	val midiAliasFiles: List<MIDIAliasFile>
		get() =
			mItems.values.filterIsInstance<MIDIAliasFile>()

	private val audioFiles: List<AudioFile>
		get() =
			mItems.values.filterIsInstance<AudioFile>()

	private val imageFiles: List<ImageFile>
		get() =
			mItems.values.filterIsInstance<ImageFile>()

	fun writeToXML(doc: Document, root: Element) =
		mItems.values.forEach { item ->
			doc.createElement(item::class.findAnnotation<CacheXmlTag>()!!.mTag).also {
				item.writeToXML(doc, it)
				root.appendChild(it)
			}
		}

	fun add(cachedItem: CachedItem) {
		mItems[cachedItem.mID] = cachedItem
	}

	fun updateLocations(fileInfo: FileInfo) {
		mItems[fileInfo.mID]?.mSubfolderIDs = fileInfo.mSubfolderIDs
	}

	fun remove(file: ItemInfo) = mItems.remove(file.mID)

	fun compareWithCacheVersion(file: FileInfo): CacheComparisonResult =
		mItems[file.mID]?.let { matchedItem ->
			if (matchedItem is CachedFile && matchedItem.mLastModified != file.mLastModified)
				CacheComparisonResult.Newer
			else if (file.mSubfolderIDs.size != matchedItem.mSubfolderIDs.size ||
				!file.mSubfolderIDs.containsAll(matchedItem.mSubfolderIDs)
			)
				CacheComparisonResult.Relocated
			else
				CacheComparisonResult.Same
		} ?: CacheComparisonResult.Newer

	fun removeNonExistent(storageIDs: Set<String>) {
		// Delete no-longer-existing files.
		mItems.values.filterIsInstance<CachedFile>().filterNot { storageIDs.contains(it.mID) }
			.forEach { f ->
				if (!f.mFile.delete())
					Logger.log { "Failed to delete file: ${f.mFile.name}" }
			}
		// Keep remaining files.
		mItems = mItems.filter { storageIDs.contains(it.value.mID) }.toMutableMap()
	}

	fun clear() = mItems.clear()

	private inline fun <reified TCachedFileType : CachedFile> getMappedFiles(
		files: List<CachedFile>,
		filenames: Array<out String>
	): List<TCachedFileType> = filenames.map {
		files.filter { file ->
			file.mNormalizedName.equals(it.normalize(), ignoreCase = true)
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
					addAll(fileToRefresh.mAudioFiles.flatMap {
						it.value.flatMap { audioFile ->
							getMappedAudioFiles(
								audioFile
							)
						}
					})
					addAll(fileToRefresh.mImageFiles.flatMap { getMappedImageFiles(it) })
				}
			}
		}

	private fun getParentFolderIDs(subfolderID: String): Set<String> =
		// Get subfolder IDs ...
		((mItems[subfolderID] as? CachedFolder)?.mSubfolderIDs ?: listOf())
			.toMutableSet().also { resultSet ->
				resultSet.toSet().forEach {
					resultSet.addAll(getParentFolderIDs(it))
				}
			}


	fun isFilterOnly(file: SongFile): Boolean =
		if (file.mFilterOnly)
			true
		else
		// Now ... if any of the folders that the file is in contain a file called ".filter_only"
		// then we treat this file as "filter only". This also applies to any parent folders of
		// those folders.
			file.mSubfolderIDs.toMutableSet().apply {
				toSet().forEach {
					addAll(getParentFolderIDs(it))
				}
			}.let { folderIDs ->
				mItems.values.any {
					it.mName.equals(
						FILTER_ONLY_FILENAME,
						true
					) && it.mSubfolderIDs.intersect(folderIDs).isNotEmpty()
				}
			}

	private fun getSubfolderIDs(folderID: String): Set<String> =
		folders.filter { it.mSubfolderIDs.contains(folderID) }.map { it.mID }.toMutableSet().apply {
			toSet().forEach {
				addAll(getSubfolderIDs(it))
			}
		}

	fun getSongsInFolder(folder: CachedFolder): List<SongFile> =
		mutableSetOf(folder.mID).apply {
			toSet().forEach {
				addAll(getSubfolderIDs(it))
			}
		}.let { folderIDs ->
			songFiles.filter { it.mSubfolderIDs.intersect(folderIDs).isNotEmpty() }
		}

	internal val midiAliases: List<Alias>
		get() = midiAliasFiles.flatMap { it.mAliasSet.aliases }.toList()

	companion object {
		const val FILTER_ONLY_FILENAME = ".filter_only"
	}
}
