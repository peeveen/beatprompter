package com.stevenfrew.beatprompter.cloud

import org.w3c.dom.Element
import java.util.*

open class CloudFileInfo(id: String, name: String, var mLastModified: Date,
// Name of the immediate subfolder that it comes from, for filtering purposes.
                         var mSubfolder: String?) : CloudItemInfo(id, name) {

    constructor(element: Element) : this(element.getAttribute(CLOUD_FILE_STORAGE_ID_ATTRIBUTE_NAME),
            element.getAttribute(CLOUD_FILE_NAME_ATTRIBUTE_NAME),
            Date(element.getAttribute(CLOUD_FILE_LAST_MODIFIED_ATTRIBUTE_NAME).toLong()),
            element.getAttribute(CLOUD_FILE_SUBFOLDER_ATTRIBUTE_NAME))

    open fun writeToXML(element: Element) {
        element.setAttribute(CLOUD_FILE_NAME_ATTRIBUTE_NAME, mName)
        element.setAttribute(CLOUD_FILE_STORAGE_ID_ATTRIBUTE_NAME, mID)
        element.setAttribute(CLOUD_FILE_LAST_MODIFIED_ATTRIBUTE_NAME, "" + mLastModified.time)
        element.setAttribute(CLOUD_FILE_SUBFOLDER_ATTRIBUTE_NAME, if (mSubfolder == null) "" else mSubfolder)
    }

    companion object {
        private const val CLOUD_FILE_NAME_ATTRIBUTE_NAME = "name"
        private const val CLOUD_FILE_STORAGE_ID_ATTRIBUTE_NAME = "storageID"
        private const val CLOUD_FILE_LAST_MODIFIED_ATTRIBUTE_NAME = "lastModified"
        private const val CLOUD_FILE_SUBFOLDER_ATTRIBUTE_NAME = "subfolder"
    }
}
