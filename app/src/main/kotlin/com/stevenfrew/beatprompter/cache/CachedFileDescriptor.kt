package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.storage.FileInfo
import org.w3c.dom.Element
import java.io.File
import java.util.*

/**
 * A description of a cached storage file. Basically a file on the filesystem, and relevant info about
 * it's source.
 */
open class CachedFileDescriptor : FileInfo {
    val mFile: File

    constructor(file: File, id: String, name: String, lastModified: Date, subfolder: String?) : super(id, name, lastModified, subfolder) {
        mFile = file
    }

    constructor(file: File, fileInfo: FileInfo) : this(file, fileInfo.mID, fileInfo.mName, fileInfo.mLastModified, fileInfo.mSubfolder)

    constructor(element: Element) : super(element) {
        mFile = File(element.getAttribute(CACHED_FILE_PATH_ATTRIBUTE_NAME))
    }

    final override fun writeToXML(element: Element) {
        super.writeToXML(element)
        element.setAttribute(CACHED_FILE_PATH_ATTRIBUTE_NAME, mFile.absolutePath)
    }

    companion object {
        private const val CACHED_FILE_PATH_ATTRIBUTE_NAME = "path"
    }
}