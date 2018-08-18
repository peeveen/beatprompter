package com.stevenfrew.beatprompter.cache

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.cache.parse.*
import com.stevenfrew.beatprompter.cloud.CloudFileInfo
import com.stevenfrew.beatprompter.midi.Alias
import org.w3c.dom.Document
import org.w3c.dom.Element

class CachedCloudFileCollection {
    private var mFiles = mutableListOf<CachedCloudFile>()

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

    private fun <TCachedCloudFileType:CachedCloudFile>addToCollection(xmlDoc:Document,tagName:String,parser: (element: Element) -> TCachedCloudFileType)
    {
        val elements = xmlDoc.getElementsByTagName(tagName)
        for (f in 0 until elements.length) {
            val element = elements.item(f) as Element
            try {
                add(parser(element))
            } catch (ibpfe: InvalidBeatPrompterFileException) {
                // This should never happen. If we could write out the file info, then it was valid.
                // So it must still be valid when we come to read it in. Unless some dastardly devious sort
                // has meddled with files outside of the app ...
                Log.d(BeatPrompterApplication.TAG, "Failed to parse file.")
                // File has become irrelevant
                add(IrrelevantFile(element))
            }
        }
    }

    fun readFromXML(xmlDoc: Document) {
        clear()

        addToCollection(xmlDoc,AudioFile.AUDIOFILE_ELEMENT_TAG_NAME) { element:Element -> AudioFileParser(CachedCloudFileDescriptor(element)).parse()}
        addToCollection(xmlDoc,ImageFile.IMAGEFILE_ELEMENT_TAG_NAME) { element:Element -> ImageFileParser(CachedCloudFileDescriptor(element)).parse()}
        addToCollection(xmlDoc,SongFile.SONGFILE_ELEMENT_TAG_NAME) { element:Element -> SongInfoParser(CachedCloudFileDescriptor(element),audioFiles,imageFiles).parse()}
        addToCollection(xmlDoc,SetListFile.SETLISTFILE_ELEMENT_TAG_NAME) { element:Element -> SetFileParser(CachedCloudFileDescriptor(element)).parse()}
        addToCollection(xmlDoc,MIDIAliasFile.MIDIALIASFILE_ELEMENT_TAG_NAME) { element:Element -> MIDIAliasFileParser(CachedCloudFileDescriptor(element)).parse()}
        addToCollection(xmlDoc,IrrelevantFile.IRRELEVANTFILE_ELEMENT_TAG_NAME) { element:Element -> IrrelevantFile(element)}
    }

    fun add(cachedFile: CachedCloudFile) {
        for (f in mFiles.indices.reversed()) {
            val existingFile = mFiles[f]
            if (cachedFile.mID.equals(existingFile.mID, ignoreCase = true)) {
                mFiles[f] = cachedFile
                return
            }
        }
        mFiles.add(cachedFile)
    }

    fun remove(cloudFile: CloudFileInfo) {
        mFiles.removeIf{cloudFile.mID.equals(it.mID, ignoreCase = true)}
    }

    fun hasLatestVersionOf(cloudFile: CloudFileInfo): Boolean {
        return mFiles.any{it.mID.equals(cloudFile.mID, ignoreCase = true) && it.mLastModified == cloudFile.mLastModified }
    }

    fun removeNonExistent(storageIDs: Set<String>) {
        val remainingFiles=mFiles.filter{storageIDs.contains(it.mID)}
        val noLongerExistingFiles=mFiles.filter{!storageIDs.contains(it.mID)}
        noLongerExistingFiles.forEach { f ->
            if (!f.mFile.delete())
                Log.e(BeatPrompterApplication.TAG, "Failed to delete file: " + f.mFile.name)
        }
        mFiles = remainingFiles.toMutableList()
    }

    fun clear() {
        mFiles.clear()
    }

    fun getMappedAudioFile(inStr: String, tempAudioFileCollection: List<AudioFile> = mutableListOf()): AudioFile? {
        val apostropheDoubleCheck=inStr.replace('’', '\'')
        return audioFiles.firstOrNull{it.mName.equals(inStr,ignoreCase=true) || it.mName.equals(apostropheDoubleCheck,ignoreCase=true)}?:
        tempAudioFileCollection.firstOrNull{it.mName.equals(inStr,ignoreCase=true) || it.mName.equals(apostropheDoubleCheck,ignoreCase=true)}
    }

    fun getMappedImageFile(inStr: String, tempImageFileCollection: List<ImageFile> = mutableListOf()): ImageFile? {
        val apostropheDoubleCheck=inStr.replace('’', '\'')
        return imageFiles.firstOrNull{it.mName.equals(inStr,ignoreCase=true) || it.mName.equals(apostropheDoubleCheck,ignoreCase=true)}?:
        tempImageFileCollection.firstOrNull{it.mName.equals(inStr,ignoreCase=true) || it.mName.equals(apostropheDoubleCheck,ignoreCase=true)}
    }

    fun getFilesToRefresh(fileToRefresh: CachedCloudFile?, includeDependencies: Boolean): List<CachedCloudFile> {
        val filesToRefresh = mutableListOf<CachedCloudFile>()
        if (fileToRefresh != null) {
            filesToRefresh.add(fileToRefresh)
            if (fileToRefresh is SongFile && includeDependencies) {
                filesToRefresh.addAll(fileToRefresh.mAudioFiles.mapNotNull {getMappedAudioFile(it)})
                filesToRefresh.addAll(fileToRefresh.mImageFiles.mapNotNull {getMappedImageFile(it)})
            }
        }
        return filesToRefresh
    }

    internal val midiAliases: List<Alias>
        get() {
            return midiAliasFiles.flatMap { it.mAliasSet.aliases}.toList()
        }
}
