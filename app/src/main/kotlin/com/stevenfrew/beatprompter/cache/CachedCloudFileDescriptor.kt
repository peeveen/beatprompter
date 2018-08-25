package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cloud.CloudFileInfo
import org.w3c.dom.Element
import java.io.File
import java.util.*

open class CachedCloudFileDescriptor:CloudFileInfo {
    val mFile: File

    constructor(file: File, id: String, name: String, lastModified: Date, subfolder: String?):super(id,name,lastModified,subfolder)
    {
        mFile=file
    }
    constructor(file: File, cloudFileInfo:CloudFileInfo):this(file,cloudFileInfo.mID,cloudFileInfo.mName,cloudFileInfo.mLastModified,cloudFileInfo.mSubfolder)

    constructor(element:Element):super(element)
    {
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