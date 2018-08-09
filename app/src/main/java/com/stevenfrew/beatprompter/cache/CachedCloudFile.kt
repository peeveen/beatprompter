package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cloud.CloudDownloadResult
import com.stevenfrew.beatprompter.cloud.CloudFileInfo
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.util.*

abstract class CachedCloudFile : CloudFileInfo {
    @JvmField var mFile: File

    internal constructor(file: File, id: String, name: String, lastModified: Date, subfolder: String):super(id,name,lastModified,subfolder)
    {
        mFile=file;
    }

    internal constructor(file: File, cloudFileInfo: CloudFileInfo) : this(file,cloudFileInfo.mID, cloudFileInfo.mName, cloudFileInfo.mLastModified, cloudFileInfo.mSubfolder)

    internal constructor(result: CloudDownloadResult) : this(result.mDownloadedFile, result.mCloudFileInfo)

    internal constructor(element: Element) : super(element) {
        mFile = File(element.getAttribute(CACHED_FILE_PATH_ATTRIBUTE_NAME))
    }

    abstract fun writeToXML(d: Document, element: Element)

    override fun writeToXML(element: Element) {
        super.writeToXML(element)
        element.setAttribute(CACHED_FILE_PATH_ATTRIBUTE_NAME, mFile.absolutePath)
    }

    companion object {

        private const val CACHED_FILE_PATH_ATTRIBUTE_NAME = "path"

        fun getTokenValue(line: String, lineNumber: Int, vararg tokens: String): String? {
            val values = getTokenValues(line, lineNumber, *tokens)
            return if (values.size == 0) null else values[values.size - 1]
        }

        internal fun getTokenValues(line: String, lineNumber: Int, vararg tokens: String): ArrayList<String> {
            val tagsOut = ArrayList<Tag>()
            val values = ArrayList<String>()
            if (!line.trim { it <= ' ' }.startsWith("#")) {
                Tag.extractTags(line, lineNumber, tagsOut)
                for (tag in tagsOut)
                    for (token in tokens)
                        if (tag.mName == token)
                            values.add(tag.mValue.trim { it <= ' ' })
            }
            return values
        }

        internal fun containsToken(line: String, lineNumber: Int, tokenToFind: String): Boolean {
            val tagsOut = ArrayList<Tag>()
            if (!line.trim { it <= ' ' }.startsWith("#")) {
                Tag.extractTags(line, lineNumber, tagsOut)
                for (tag in tagsOut)
                    if (tag.mName == tokenToFind)
                        return true
            }
            return false
        }

        fun createCachedCloudFile(result: CloudDownloadResult): CachedCloudFile? {
            try {
                return AudioFile(result)
            } catch (ioe: InvalidBeatPrompterFileException) {
                try {
                    return ImageFile(result)
                } catch (ibpfe1: InvalidBeatPrompterFileException) {
                    try {
                        return MIDIAliasFile(result)
                    } catch (ibpfe2: InvalidBeatPrompterFileException) {
                        try {
                            return SongFile(result)
                        } catch (ibpfe3: InvalidBeatPrompterFileException) {
                            try {
                                return SetListFile(result)
                            } catch (ibpfe4: InvalidBeatPrompterFileException) {
                                // Not any kind of file we're interested in?
                                // We don't want this file.
                            }
                        }
                    }
                }
            }
            return null
        }
    }
}