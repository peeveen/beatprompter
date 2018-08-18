package com.stevenfrew.beatprompter.cache

import android.graphics.BitmapFactory
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.InvalidBeatPrompterFileException
import com.stevenfrew.beatprompter.cloud.CloudFileInfo
import com.stevenfrew.beatprompter.cloud.SuccessfulCloudDownloadResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File

class ImageFile : CachedCloudFile {

    internal constructor(cachedCloudFileDescriptor:CachedCloudFileDescriptor) : super(cachedCloudFileDescriptor)
    internal constructor(element: Element) : super(element)

    override fun writeToXML(d: Document, element: Element) {
        val imageFileElement = d.createElement(IMAGEFILE_ELEMENT_TAG_NAME)
        super.writeToXML(imageFileElement)
        element.appendChild(imageFileElement)
    }

    companion object {
        const val IMAGEFILE_ELEMENT_TAG_NAME = "imagefile"
    }
}
