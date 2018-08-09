package com.stevenfrew.beatprompter.cache

import android.media.MediaMetadataRetriever
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult
import org.w3c.dom.Document
import org.w3c.dom.Element

class AudioFile : CachedCloudFile {

    @Throws(InvalidBeatPrompterFileException::class)
    internal constructor(result: CloudDownloadResult) : super(result) {
        verifyAudioFile()
    }

    internal constructor(e: Element) : super(e)

    override fun writeToXML(d: Document, element: Element) {
        val audioFileElement = d.createElement(AUDIOFILE_ELEMENT_TAG_NAME)
        super.writeToXML(audioFileElement)
        element.appendChild(audioFileElement)
    }

    @Throws(InvalidBeatPrompterFileException::class)
    private fun verifyAudioFile() {
        try {
            // Try to read the length of the track. If it fails, it's not an audio file.
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(mFile.absolutePath)
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        } catch (e: Exception) {
            throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.notAnAudioFile, mName))
        }

    }

    companion object {
        const val AUDIOFILE_ELEMENT_TAG_NAME = "audiofile"
    }
}
