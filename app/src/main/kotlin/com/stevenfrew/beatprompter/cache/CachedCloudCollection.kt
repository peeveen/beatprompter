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
    private var mItems = mutableListOf<CachedItem>()

    val songFiles: List<SongFile>
        get() =
            mItems.filterIsInstance<SongFile>()

    val setListFiles: List<SetListFile>
        get() =
            mItems.filterIsInstance<SetListFile>()

    val midiAliasFiles: List<MIDIAliasFile>
        get() =
            mItems.filterIsInstance<MIDIAliasFile>()

    private val audioFiles: List<AudioFile>
        get() =
            mItems.filterIsInstance<AudioFile>()

    private val imageFiles: List<ImageFile>
        get() =
            mItems.filterIsInstance<ImageFile>()

    fun writeToXML(doc: Document, root: Element) {
        for (ccf in mItems) {
            doc.createElement(ccf::class.annotations.filterIsInstance<CacheXmlTag>().first().mTag)
                    .also {
                        ccf.writeToXML(doc, it)
                        root.appendChild(it)
                    }
        }
    }

    private fun <TCachedCloudFileType : CachedFile> addToCollection(xmlDoc: Document,
                                                                    tagName: String,
                                                                    parser: (cachedItem: Element) -> TCachedCloudFileType) {
        val elements = xmlDoc.getElementsByTagName(tagName)
        val folderTagName = CachedFolder::class.annotations.filterIsInstance<CacheXmlTag>().first().mTag
        repeat(elements.length) {
            val element = elements.item(it) as Element
            try {
                if (element.tagName == folderTagName)
                    add(CachedFolder(element))
                else
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
        for (f in mItems.indices.reversed()) {
            val existingFile = mItems[f]
            if (cachedItem.mID.equals(existingFile.mID, ignoreCase = true)) {
                mItems[f] = cachedItem
                return
            }
        }
        mItems.add(cachedItem)
    }

    fun remove(file: ItemInfo) {
        mItems.removeAll { file.mID.equals(it.mID, ignoreCase = true) }
    }

    fun hasLatestVersionOf(file: FileInfo): Boolean {
        return mItems.filterIsInstance<CachedFile>().any {
            it.mID.equals(file.mID, ignoreCase = true)
                    && it.mLastModified == file.mLastModified
        }
    }

    fun removeNonExistent(storageIDs: Set<String>) {
        // Delete no-longer-existing files.
        mItems.filterIsInstance<CachedFile>().filter { !storageIDs.contains(it.mID) }.forEach { f ->
            if (!f.mFile.delete())
                Logger.log { "Failed to delete file: ${f.mFile.name}" }
        }
        // Keep remaining files.
        mItems = mItems.filter { storageIDs.contains(it.mID) }.toMutableList()
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

    fun getFolderName(folderID: String): String? {
        return mItems
                .filterIsInstance<CachedFolder>()
                .firstOrNull { it.mID == folderID }?.mName
    }

    internal val midiAliases: List<Alias>
        get() {
            return midiAliasFiles.flatMap { it.mAliasSet.aliases }.toList()
        }
}
