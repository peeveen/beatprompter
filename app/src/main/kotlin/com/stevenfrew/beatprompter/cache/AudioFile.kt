package com.stevenfrew.beatprompter.cache

import android.media.MediaMetadataRetriever
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.InvalidBeatPrompterFileException
import com.stevenfrew.beatprompter.cloud.SuccessfulCloudDownloadResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File

class AudioFile : CachedCloudFile {

    @Throws(InvalidBeatPrompterFileException::class)
    internal constructor(result: SuccessfulCloudDownloadResult) : super(result) {
        verifyAudioFile(mFile,mName)
    }

    internal constructor(e: Element) : super(e)

    override fun writeToXML(d: Document, element: Element) {
        val audioFileElement = d.createElement(AUDIOFILE_ELEMENT_TAG_NAME)
        super.writeToXML(audioFileElement)
        element.appendChild(audioFileElement)
    }

    companion object {
        const val AUDIOFILE_ELEMENT_TAG_NAME = "audiofile"

        @Throws(InvalidBeatPrompterFileException::class)
        private fun verifyAudioFile(file: File,name:String) {
            try {
                // Try to read the length of the track. If it fails, it's not an audio file.
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(file.absolutePath)
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            } catch (e: Exception) {
                throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.notAnAudioFile, name))
            }
        }
    }
}
