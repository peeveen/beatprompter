package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.*
import com.stevenfrew.beatprompter.midi.*
import org.w3c.dom.Document
import org.w3c.dom.Element

class MIDIAliasFile internal constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor, aliasSet: AliasSet, errors: List<FileParseError>) : CachedCloudTextFile(cachedCloudFileDescriptor,errors) {
    val mAliasSet:AliasSet = aliasSet

    override fun writeToXML(d: Document, element: Element) {
        val aliasFileElement = d.createElement(MIDIALIASFILE_ELEMENT_TAG_NAME)
        super.writeToXML(aliasFileElement)
        element.appendChild(aliasFileElement)
    }

    companion object {
        const val MIDIALIASFILE_ELEMENT_TAG_NAME = "midialiases"
    }
}
