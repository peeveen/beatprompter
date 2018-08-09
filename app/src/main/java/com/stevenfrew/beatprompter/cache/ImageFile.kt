package com.stevenfrew.beatprompter.cache

import android.graphics.BitmapFactory
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult
import org.w3c.dom.Document
import org.w3c.dom.Element

class ImageFile : CachedCloudFile {

    @Throws(InvalidBeatPrompterFileException::class)
    internal constructor(result: CloudDownloadResult) : super(result) {
        verifyImageFile()
    }

    internal constructor(element: Element) : super(element)

    override fun writeToXML(d: Document, element: Element) {
        val imageFileElement = d.createElement(IMAGEFILE_ELEMENT_TAG_NAME)
        super.writeToXML(imageFileElement)
        element.appendChild(imageFileElement)
    }

    @Throws(InvalidBeatPrompterFileException::class)
    private fun verifyImageFile() {
        val options = BitmapFactory.Options()
        try {
            BitmapFactory.decodeFile(mFile.absolutePath, options)
                    ?: throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.could_not_read_image_file) + ": " + mName)
        } catch (e: Exception) {
            throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.could_not_read_image_file) + ": " + mName)
        }

    }

    companion object {
        const val IMAGEFILE_ELEMENT_TAG_NAME = "imagefile"
    }
}
