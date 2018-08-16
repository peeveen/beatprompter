package com.stevenfrew.beatprompter.cache

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.cache.parse.InvalidBeatPrompterFileException
import com.stevenfrew.beatprompter.cloud.CloudFileInfo
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File

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

    fun readFromXML(xmlDoc: Document) {
        clear()

        val songFiles = xmlDoc.getElementsByTagName(SongFile.SONGFILE_ELEMENT_TAG_NAME)
        for (f in 0 until songFiles.length)
            try {
                add(SongFile(songFiles.item(f) as Element))
            } catch (ibpfe: InvalidBeatPrompterFileException) {
                // This should never happen. If we could write out the file info, then it was valid.
                // So it must still be valid when we come to read it in. Unless some dastardly devious sort
                // has meddled with files outside of the app ...
                Log.d(BeatPrompterApplication.TAG, "Failed to parse song file.")
            }

        val setFiles = xmlDoc.getElementsByTagName(SetListFile.SETLISTFILE_ELEMENT_TAG_NAME)
        for (f in 0 until setFiles.length)
            try {
                add(SetListFile(setFiles.item(f) as Element))
            } catch (ibpfe: InvalidBeatPrompterFileException) {
                // This should never happen. If we could write out the file info, then it was valid.
                // So it must still be valid when we come to read it in. Unless some dastardly devious sort
                // has meddled with files outside of the app ...
                Log.d(BeatPrompterApplication.TAG, "Failed to parse set-list file.")
            }

        val imageFiles = xmlDoc.getElementsByTagName(ImageFile.IMAGEFILE_ELEMENT_TAG_NAME)
        for (f in 0 until imageFiles.length)
            add(ImageFile(imageFiles.item(f) as Element))

        val audioFiles = xmlDoc.getElementsByTagName(AudioFile.AUDIOFILE_ELEMENT_TAG_NAME)
        for (f in 0 until audioFiles.length)
            add(AudioFile(audioFiles.item(f) as Element))

        val aliasFiles = xmlDoc.getElementsByTagName(MIDIAliasFile.MIDIALIASFILE_ELEMENT_TAG_NAME)
        for (f in 0 until aliasFiles.length)
            try {
                add(MIDIAliasFile(aliasFiles.item(f) as Element))
            } catch (ibpfe: InvalidBeatPrompterFileException) {
                // This should never happen. If we could write out the file info, then it was valid.
                // So it must still be valid when we come to read it in. Unless some dastardly devious sort
                // has meddled with files outside of the app ...
                Log.d(BeatPrompterApplication.TAG, "Failed to parse MIDI alias file.")
            }

        val irrelevantFiles = xmlDoc.getElementsByTagName(IrrelevantFile.IRRELEVANTFILE_ELEMENT_TAG_NAME)
        for (f in 0 until irrelevantFiles.length)
            add(IrrelevantFile(irrelevantFiles.item(f) as Element))
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

    fun getMappedAudioFilename(inStr: String, tempAudioFileCollection: List<AudioFile> = mutableListOf()): AudioFile? {
        val apostropheDoubleCheck=inStr.replace('’', '\'')
        return audioFiles.find{it.mName.equals(inStr,ignoreCase=true) || it.mName.equals(apostropheDoubleCheck,ignoreCase=true)}?:
        tempAudioFileCollection.find{it.mName.equals(inStr,ignoreCase=true) || it.mName.equals(apostropheDoubleCheck,ignoreCase=true)}
    }

    fun getMappedImageFilename(inStr: String, tempImageFileCollection: List<ImageFile> = mutableListOf()): ImageFile? {
        val apostropheDoubleCheck=inStr.replace('’', '\'')
        return imageFiles.find{it.mName.equals(inStr,ignoreCase=true) || it.mName.equals(apostropheDoubleCheck,ignoreCase=true)}?:
        tempImageFileCollection.find{it.mName.equals(inStr,ignoreCase=true) || it.mName.equals(apostropheDoubleCheck,ignoreCase=true)}
    }

    fun getFilesToRefresh(fileToRefresh: CachedCloudFile?, includeDependencies: Boolean): List<CachedCloudFile> {
        val filesToRefresh = mutableListOf<CachedCloudFile>()
        if (fileToRefresh != null) {
            filesToRefresh.add(fileToRefresh)
            if (fileToRefresh is SongFile && includeDependencies) {
                filesToRefresh.addAll(fileToRefresh.mAudioFiles.mapNotNull{getMappedAudioFilename(it)}.filter{File(fileToRefresh.mFile.parent,it.mFile.name).exists()})
                filesToRefresh.addAll(fileToRefresh.mImageFiles.mapNotNull{getMappedImageFilename(it)}.filter{File(fileToRefresh.mFile.parent,it.mFile.name).exists()})
            }
        }
        return filesToRefresh
    }
}
