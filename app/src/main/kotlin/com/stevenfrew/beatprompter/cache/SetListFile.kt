package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.*
import org.w3c.dom.Document
import org.w3c.dom.Element

class SetListFile internal constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor, val mSetTitle:String, val mSongTitles:MutableList<String>, errors: List<FileParseError>) : CachedCloudTextFile(cachedCloudFileDescriptor,errors) {
    override fun writeToXML(d: Document, element: Element) {
        val setListFileElement = d.createElement(SETLISTFILE_ELEMENT_TAG_NAME)
        super.writeToXML(setListFileElement)
        setListFileElement.setAttribute(SET_TITLE_ATTRIBUTE_NAME, mSetTitle)
        element.appendChild(setListFileElement)
    }

    companion object {
        const val SETLISTFILE_ELEMENT_TAG_NAME = "set"
        private const val SET_TITLE_ATTRIBUTE_NAME = "title"
    }
}
