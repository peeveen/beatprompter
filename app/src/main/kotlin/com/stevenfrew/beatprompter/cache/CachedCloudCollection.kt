package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.cache.parse.*
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

    fun writeToXML(doc: Document, root: Element) {
        for (item in mItems.values) {
            doc.createElement(item::class.annotations.filterIsInstance<CacheXmlTag>().first().mTag)
                    .also {
                        item.writeToXML(doc, it)
                        root.appendChild(it)
                    }
        }
    }

    private fun <TCachedCloudItemType : CachedItem> addToCollection(xmlDoc: Document,
                                                                    tagName: String,
                                                                    parser: (cachedItem: Element) -> TCachedCloudItemType) {
        val elements = xmlDoc.getElementsByTagName(tagName)
        val folderTagName = CachedFolder::class.annotations.filterIsInstance<CacheXmlTag>().first().mTag
        repeat(elements.length) {
            val element = elements.item(it) as Element
            try {
                add(parser(element))
            } catch (ibpfe: InvalidBeatPrompterFileException) {
                // This should never happen. If we could write out the file info, then it was valid.
                // So it must still be valid when we come to read it in. Unless some dastardly devious sort
                // has meddled with files outside of the app ...
                Logger.log("Failed to parse file.")
                // File has become irrelevant
                add(IrrelevantFile(CachedFile(element)))
            }
        }
    }

    fun readFromXML(xmlDoc: Document) {
        clear()

        addToCollection(xmlDoc, CachedFolder::class.findAnnotation<CacheXmlTag>()!!.mTag) { element ->
            CachedFolder(element)
        }
        addToCollection(xmlDoc, AudioFile::class.findAnnotation<CacheXmlTag>()!!.mTag) { element ->
            AudioFileParser(CachedFile(element)).parse()
        }
        addToCollection(xmlDoc, ImageFile::class.findAnnotation<CacheXmlTag>()!!.mTag) { element ->
            ImageFileParser(CachedFile(element)).parse()
        }
        addToCollection(xmlDoc, SongFile::class.findAnnotation<CacheXmlTag>()!!.mTag) { element ->
            SongInfoParser(CachedFile(element)).parse()
        }
        addToCollection(xmlDoc, SetListFile::class.findAnnotation<CacheXmlTag>()!!.mTag) { element ->
            SetListFileParser(CachedFile(element)).parse()
        }
        addToCollection(xmlDoc, MIDIAliasFile::class.findAnnotation<CacheXmlTag>()!!.mTag) { element ->
            MIDIAliasFileParser(CachedFile(element)).parse()
        }
        addToCollection(xmlDoc, IrrelevantFile::class.findAnnotation<CacheXmlTag>()!!.mTag) { element ->
            IrrelevantFile(CachedFile(element))
        }
    }

    fun add(cachedItem: CachedItem) {
        mItems[cachedItem.mID] = cachedItem
    }

    fun updateLocations(fileInfo: FileInfo) {
        val existingItem = mItems[fileInfo.mID]
        existingItem?.mSubfolderIDs = fileInfo.mSubfolderIDs
    }

    fun remove(file: ItemInfo) {
        mItems.remove(file.mID)
    }

    fun compareWithCacheVersion(file: FileInfo): CacheComparisonResult {
        val matchedItem = mItems[file.mID] ?: return CacheComparisonResult.Newer
        if (matchedItem is CachedFile && matchedItem.mLastModified != file.mLastModified)
            return CacheComparisonResult.Newer
        if (file.mSubfolderIDs.size != matchedItem.mSubfolderIDs.size || !file.mSubfolderIDs.containsAll(matchedItem.mSubfolderIDs))
            return CacheComparisonResult.Relocated
        return CacheComparisonResult.Same
    }

    fun removeNonExistent(storageIDs: Set<String>) {
        // Delete no-longer-existing files.
        mItems.values.filterIsInstance<CachedFile>().filter { !storageIDs.contains(it.mID) }.forEach { f ->
            if (!f.mFile.delete())
                Logger.log { "Failed to delete file: ${f.mFile.name}" }
        }
        // Keep remaining files.
        mItems = mItems.filter { storageIDs.contains(it.value.mID) }.toMutableMap()
    }

    fun clear() {
        mItems.clear()
    }

    fun getMappedAudioFiles(vararg inStrs: String): List<AudioFile> {
        return inStrs
                .map {
                    audioFiles
                            .filter { audioFile ->
                                audioFile.mNormalizedName.equals(it.normalize(), ignoreCase = true)
                            }
                }
                .flattenAll()
                .filterIsInstance<AudioFile>()
    }

    fun getMappedImageFiles(vararg inStrs: String): List<ImageFile> {
        return inStrs
                .map {
                    imageFiles
                            .filter { imageFile ->
                                imageFile.mNormalizedName.equals(it.normalize(), ignoreCase = true)
                            }
                }
                .flattenAll()
                .filterIsInstance<ImageFile>()
    }

    fun getFilesToRefresh(fileToRefresh: CachedFile?, includeDependencies: Boolean): List<CachedFile> {
        val filesToRefresh = mutableListOf<CachedFile>()
        if (fileToRefresh != null) {
            filesToRefresh.add(fileToRefresh)
            if (fileToRefresh is SongFile && includeDependencies) {
                filesToRefresh.addAll(fileToRefresh.mAudioFiles.flatMap { getMappedAudioFiles(it) })
                filesToRefresh.addAll(fileToRefresh.mImageFiles.flatMap { getMappedImageFiles(it) })
            }
        }
        return filesToRefresh
    }

    private fun getParentFolderIDs(subfolderID: String): Set<String> {
        val subfolderIDs = (mItems[subfolderID] as? CachedFolder)?.mSubfolderIDs ?: listOf()
        return subfolderIDs.toMutableSet().also { resultSet ->
            resultSet.toSet().forEach {
                resultSet.addAll(getParentFolderIDs(it))
            }
        }
    }

    fun isFilterOnly(file: SongFile): Boolean {
        if (file.mFilterOnly)
            return true
        // Now ... if any of the folders that the file is in contain a file called ".filter_only"
        // then we treat this file as "filter only". This also applies to any parent folders of
        // those folders.
        val folderIDs = file.mSubfolderIDs.toMutableSet()
        folderIDs.toSet().forEach {
            folderIDs.addAll(getParentFolderIDs(it))
        }
        return mItems.values.any { it.mName.equals(".filter_only", true) && it.mSubfolderIDs.intersect(folderIDs).isNotEmpty() }
    }

    private fun getSubfolderIDs(folderID: String): Set<String> {
        val subfolderIDs = folders.filter { it.mSubfolderIDs.contains(folderID) }.map { it.mID }.toMutableSet()
        subfolderIDs.toSet().forEach {
            subfolderIDs.addAll(getSubfolderIDs(it))
        }
        return subfolderIDs
    }

    fun getSongsInFolder(folder: CachedFolder): List<SongFile> {
        val subfolderIDs = mutableSetOf(folder.mID)
        subfolderIDs.toSet().forEach {
            subfolderIDs.addAll(getSubfolderIDs(it))
        }
        return songFiles.filter { it.mSubfolderIDs.intersect(subfolderIDs).isNotEmpty() }
    }

    internal val midiAliases: List<Alias>
        get() {
            return midiAliasFiles.flatMap { it.mAliasSet.aliases }.toList()
        }
}
