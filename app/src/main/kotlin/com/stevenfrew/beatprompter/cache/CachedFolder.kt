package com.stevenfrew.beatprompter.cache

import org.w3c.dom.Document
import org.w3c.dom.Element

@CacheXmlTag("folder")
/**
 * A description of a storage folder. Folders are NEVER recreated locally, but we keep track of them
 * for filtering purposes.
 */
class CachedFolder : CachedItem {
    val mFilterOnly: Boolean

    constructor(id: String,
                name: String,
                filterOnly: Boolean,
                subfolderIDs: List<String>)
            : super(id, name, subfolderIDs) {
        mFilterOnly = filterOnly
    }

    constructor(element: Element) : super(element) {
        mFilterOnly = element.getAttribute(CACHED_FOLDER_FILTER_ONLY_ATTRIBUTE_NAME)!!.toBoolean()
    }

    override fun writeToXML(doc: Document, element: Element) {
        super.writeToXML(doc, element)
        element.setAttribute(CACHED_FOLDER_FILTER_ONLY_ATTRIBUTE_NAME, mFilterOnly.toString())
    }

    companion object {
        private const val CACHED_FOLDER_FILTER_ONLY_ATTRIBUTE_NAME = "filterOnly"
    }
}