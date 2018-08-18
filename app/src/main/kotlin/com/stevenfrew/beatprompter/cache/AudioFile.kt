package com.stevenfrew.beatprompter.cache

import android.media.MediaMetadataRetriever
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.InvalidBeatPrompterFileException
import com.stevenfrew.beatprompter.cloud.CloudFileInfo
import com.stevenfrew.beatprompter.cloud.SuccessfulCloudDownloadResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File

class AudioFile : CachedCloudFile {

    internal constructor(cachedCloudFileDescriptor:CachedCloudFileDescriptor) : super(cachedCloudFileDescriptor)
    internal constructor(e: Element) : super(e)

    override fun writeToXML(d: Document, element: Element) {
        val audioFileElement = d.createElement(AUDIOFILE_ELEMENT_TAG_NAME)
        super.writeToXML(audioFileElement)
        element.appendChild(audioFileElement)
    }

    companion object {
        const val AUDIOFILE_ELEMENT_TAG_NAME = "audiofile"
    }
}
