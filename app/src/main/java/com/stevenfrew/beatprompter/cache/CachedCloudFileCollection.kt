package com.stevenfrew.beatprompter.cache

import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.cloud.CloudFileInfo
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.util.ArrayList

class CachedCloudFileCollection {
    private var mFiles: MutableList<CachedCloudFile> = ArrayList()

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
        for (f in 0 until songFiles.length) {
            val n = songFiles.item(f)
            val song = SongFile(n as Element)
            add(song)
        }
        val setFiles = xmlDoc.getElementsByTagName(SetListFile.SETLISTFILE_ELEMENT_TAG_NAME)
        for (f in 0 until setFiles.length) {
            val n = setFiles.item(f)
            try {
                val set = SetListFile(n as Element)
                add(set)
            } catch (ibpfe: InvalidBeatPrompterFileException) {
                // This should never happen. If we could write out the file info, then it was valid.
                // So it must still be valid when we come to read it in. Unless some dastardly devious sort
                // has meddled with files outside of the app ...
                Log.d(BeatPrompterApplication.TAG, "Failed to parse set-list file.")
            }

        }
        val imageFiles = xmlDoc.getElementsByTagName(ImageFile.IMAGEFILE_ELEMENT_TAG_NAME)
        for (f in 0 until imageFiles.length) {
            val n = imageFiles.item(f)
            val imageFile = ImageFile(n as Element)
            add(imageFile)
        }
        val audioFiles = xmlDoc.getElementsByTagName(AudioFile.AUDIOFILE_ELEMENT_TAG_NAME)
        for (f in 0 until audioFiles.length) {
            val n = audioFiles.item(f)
            val audioFile = AudioFile(n as Element)
            add(audioFile)
        }
        val aliasFiles = xmlDoc.getElementsByTagName(MIDIAliasFile.MIDIALIASFILE_ELEMENT_TAG_NAME)
        for (f in 0 until aliasFiles.length) {
            val n = aliasFiles.item(f)
            try {
                val midiAliasCachedCloudFile = MIDIAliasFile(n as Element)
                add(midiAliasCachedCloudFile)
            } catch (ibpfe: InvalidBeatPrompterFileException) {
                // This should never happen. If we could write out the file info, then it was valid.
                // So it must still be valid when we come to read it in. Unless some dastardly devious sort
                // has meddled with files outside of the app ...
                Log.d(BeatPrompterApplication.TAG, "Failed to parse MIDI alias file.")
            }

        }
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
        for (f in mFiles.indices.reversed()) {
            val existingFile = mFiles[f]
            if (cloudFile.mID.equals(existingFile.mID, ignoreCase = true))
                mFiles.removeAt(f)
        }
    }

    fun hasLatestVersionOf(cloudFile: CloudFileInfo): Boolean {
        return mFiles.stream().anyMatch { f -> f.mID.equals(cloudFile.mID, ignoreCase = true) && f.mLastModified == cloudFile.mLastModified }
    }

    fun removeNonExistent(storageIDs: Set<String>) {
        val remainingFiles=mFiles.filter{storageIDs.contains(it.mID)}
        val noLongerExistingFiles=mFiles.filter{!storageIDs.contains(it.mID)}
//        val remainingFiles = mFiles.stream().filter { f -> storageIDs.contains(f.mID) }.collect<List<CachedCloudFile>, Any>(Collectors.toList())
//        val noLongerExistingFiles = mFiles.stream().filter { f -> !storageIDs.contains(f.mID) }.collect<List<CachedCloudFile>, Any>(Collectors.toList())
        noLongerExistingFiles.forEach { f ->
            if (!f.mFile.delete())
                Log.e(BeatPrompterApplication.TAG, "Failed to delete file: " + f.mFile.name)
        }
        mFiles = remainingFiles.toMutableList()
    }

    fun clear() {
        mFiles.clear()
    }

    fun getMappedAudioFilename(`in`: String?, tempAudioFileCollection: ArrayList<AudioFile>?): AudioFile? {
        if (`in` != null) {
            for (afm in audioFiles) {
                val secondChance = `in`.replace('’', '\'')
                if (afm.mName.equals(`in`, ignoreCase = true) || afm.mName.equals(secondChance, ignoreCase = true))
                    return afm
            }
            if (tempAudioFileCollection != null)
                for (afm in tempAudioFileCollection) {
                    val secondChance = `in`.replace('’', '\'')
                    if (afm.mName.equals(`in`, ignoreCase = true) || afm.mName.equals(secondChance, ignoreCase = true))
                        return afm
                }
        }
        return null
    }

    fun getMappedImageFilename(`in`: String?, tempImageFileCollection: ArrayList<ImageFile>?): ImageFile? {
        if (`in` != null) {
            for (ifm in imageFiles) {
                val secondChance = `in`.replace('’', '\'')
                if (ifm.mName.equals(`in`, ignoreCase = true) || ifm.mName.equals(secondChance, ignoreCase = true))
                    return ifm
            }
            if (tempImageFileCollection != null)
                for (ifm in tempImageFileCollection) {
                    val secondChance = `in`.replace('’', '\'')
                    if (ifm.mName.equals(`in`, ignoreCase = true) || ifm.mName.equals(secondChance, ignoreCase = true))
                        return ifm
                }
        }
        return null
    }

    fun getFilesToRefresh(fileToRefresh: CachedCloudFile?, includeDependencies: Boolean): ArrayList<CachedCloudFile> {
        val filesToRefresh = ArrayList<CachedCloudFile>()
        if (fileToRefresh != null) {
            filesToRefresh.add(fileToRefresh)
            if (fileToRefresh is SongFile && includeDependencies) {
                val song = fileToRefresh as SongFile?
                if(song!=null) {
                    for (audioFileName in song.mAudioFiles) {
                        val audioFile = getMappedAudioFilename(audioFileName, null)
                        var actualAudioFile: File? = null
                        if (audioFile != null)
                            actualAudioFile = File(song.mFile.parent, audioFile.mFile.name)
                        if (actualAudioFile != null && actualAudioFile.exists())
                            filesToRefresh.add(audioFile!!)
                    }
                    for (imageFileName in song.mImageFiles) {
                        val imageFile = getMappedImageFilename(imageFileName, null)
                        var actualImageFile: File? = null
                        if (imageFile != null)
                            actualImageFile = File(song.mFile.parent, imageFile.mFile.name)
                        if (actualImageFile != null && actualImageFile.exists())
                            filesToRefresh.add(imageFile!!)
                    }
                }
            }
        }
        return filesToRefresh
    }
}
