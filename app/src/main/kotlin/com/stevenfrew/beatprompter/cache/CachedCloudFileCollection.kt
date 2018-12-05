package com.stevenfrew.beatprompter.cache

import android.os.Build
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.cache.parse.*
import com.stevenfrew.beatprompter.storage.FileInfo
import com.stevenfrew.beatprompter.util.flattenAll
import com.stevenfrew.beatprompter.midi.alias.Alias
import com.stevenfrew.beatprompter.util.normalize
import org.w3c.dom.Document
import org.w3c.dom.Element
import kotlin.reflect.full.findAnnotation

/**
 * The file cache.
 */
class CachedCloudFileCollection {
    private var mFiles = mutableListOf<CachedFile>()

    val songFiles: List<SongFile>
        get() =
            mFiles.filterIsInstance<SongFile>()

    val setListFiles: List<SetListFile>
        get() =
            mFiles.filterIsInstance<SetListFile>()

    val midiAliasFiles: List<MIDIAliasFile>
        get() =
            mFiles.filterIsInstance<MIDIAliasFile>()

    private val audioFiles: List<AudioFile>
        get() =
            mFiles.filterIsInstance<AudioFile>()

    private val imageFiles: List<ImageFile>
        get() =
            mFiles.filterIsInstance<ImageFile>()

    val isEmpty: Boolean
        get() = mFiles.isEmpty()

    fun writeToXML(d: Document, root: Element) {
        for (ccf in mFiles)
            ccf.writeToXML(d, root)
    }

    private fun <TCachedCloudFileType : CachedFile> addToCollection(xmlDoc: Document,
                                                                    tagName: String,
                                                                    parser: (cachedCloudFileDescriptor: CachedFileDescriptor) -> TCachedCloudFileType) {
        val elements = xmlDoc.getElementsByTagName(tagName)
        repeat(elements.length) {
            val element = elements.item(it) as Element
            try {
                add(parser(CachedFileDescriptor(element)))
            } catch (ibpfe: InvalidBeatPrompterFileException) {
                // This should never happen. If we could write out the file info, then it was valid.
                // So it must still be valid when we come to read it in. Unless some dastardly devious sort
                // has meddled with files outside of the app ...
                Log.d(BeatPrompterApplication.TAG, "Failed to parse file.")
                // File has become irrelevant
                add(IrrelevantFile(CachedFileDescriptor(element)))
            }
        }
    }

    fun readFromXML(xmlDoc: Document) {
        clear()

        addToCollection(xmlDoc, AudioFile::class.findAnnotation<CacheXmlTag>()!!.mTag) { descriptor ->
            AudioFileParser(descriptor).parse()
        }
        addToCollection(xmlDoc, ImageFile::class.findAnnotation<CacheXmlTag>()!!.mTag) { descriptor ->
            ImageFileParser(descriptor).parse()
        }
        addToCollection(xmlDoc, SongFile::class.findAnnotation<CacheXmlTag>()!!.mTag) { descriptor ->
            SongInfoParser(descriptor).parse()
        }
        addToCollection(xmlDoc, SetListFile::class.findAnnotation<CacheXmlTag>()!!.mTag) { descriptor ->
            SetListFileParser(descriptor).parse()
        }
        addToCollection(xmlDoc, MIDIAliasFile::class.findAnnotation<CacheXmlTag>()!!.mTag) { descriptor ->
            MIDIAliasFileParser(descriptor).parse()
        }
        addToCollection(xmlDoc, IrrelevantFile::class.findAnnotation<CacheXmlTag>()!!.mTag) { descriptor ->
            IrrelevantFile(descriptor)
        }
    }

    fun add(cachedFile: CachedFile) {
        for (f in mFiles.indices.reversed()) {
            val existingFile = mFiles[f]
            if (cachedFile.mID.equals(existingFile.mID, ignoreCase = true)) {
                mFiles[f] = cachedFile
                return
            }
        }
        mFiles.add(cachedFile)
    }

    fun remove(file: FileInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            mFiles.removeIf { file.mID.equals(it.mID, ignoreCase = true) }
        else
            mFiles = mFiles
                    .asSequence()
                    .filter { !file.mID.equals(it.mID, ignoreCase = true) }
                    .toMutableList()
    }

    fun hasLatestVersionOf(file: FileInfo): Boolean {
        return mFiles.any {
            it.mID.equals(file.mID, ignoreCase = true) && it.mLastModified == file.mLastModified
        }
    }

    fun removeNonExistent(storageIDs: Set<String>) {
        val remainingFiles = mFiles.filter { storageIDs.contains(it.mID) }
        val noLongerExistingFiles = mFiles.filter { !storageIDs.contains(it.mID) }
        noLongerExistingFiles.forEach { f ->
            if (!f.mFile.delete())
                Log.e(BeatPrompterApplication.TAG, "Failed to delete file: " + f.mFile.name)
        }
        mFiles = remainingFiles.toMutableList()
    }

    fun clear() {
        mFiles.clear()
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

    internal val midiAliases: List<Alias>
        get() {
            return midiAliasFiles.flatMap { it.mAliasSet.aliases }.toList()
        }
}
