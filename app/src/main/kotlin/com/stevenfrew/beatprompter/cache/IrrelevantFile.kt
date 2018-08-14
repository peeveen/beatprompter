package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cloud.SuccessfulCloudDownloadResult
import org.w3c.dom.Document
import org.w3c.dom.Element

class IrrelevantFile : CachedCloudFile {

    @Throws(InvalidBeatPrompterFileException::class)
    internal constructor(result: SuccessfulCloudDownloadResult) : super(result)

    internal constructor(element: Element) : super(element)

    override fun writeToXML(d: Document, element: Element) {
        val irrelevantFileElement = d.createElement(IRRELEVANTFILE_ELEMENT_TAG_NAME)
        super.writeToXML(irrelevantFileElement)
        element.appendChild(irrelevantFileElement)
    }

    companion object {
        const val IRRELEVANTFILE_ELEMENT_TAG_NAME = "irrelevantfile"
    }
}
