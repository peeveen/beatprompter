package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.*
import com.stevenfrew.beatprompter.storage.SuccessfulDownloadResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.util.*

/**
 * A description of a cached storage file. Basically a file on the filesystem, and relevant info about
 * it's source.
 */
open class CachedFile : CachedItem {
    val mFile: File
    val mLastModified: Date

    constructor(file: File,
                id: String,
                name: String,
                lastModified: Date,
                subfolderIDs: List<String>)
            : super(id, name, subfolderIDs) {
        mFile = file
        mLastModified = lastModified
    }

    constructor(cachedFile: CachedFile)
            : this(cachedFile.mFile, cachedFile.mID, cachedFile.mName, cachedFile.mLastModified, cachedFile.mSubfolderIDs)

    constructor(element: Element) : super(element) {
        mLastModified = Date(element.getAttribute(CACHED_FILE_LAST_MODIFIED_ATTRIBUTE_NAME).toLong())
        mFile = File(element.getAttribute(CACHED_FILE_PATH_ATTRIBUTE_NAME))
    }

    override fun writeToXML(doc: Document, element: Element) {
        super.writeToXML(doc, element)
        element.setAttribute(CACHED_FILE_PATH_ATTRIBUTE_NAME, mFile.absolutePath)
        element.setAttribute(CACHED_FILE_LAST_MODIFIED_ATTRIBUTE_NAME, "${mLastModified.time}")
    }

    companion object {
        private const val CACHED_FILE_PATH_ATTRIBUTE_NAME = "path"
        private const val CACHED_FILE_LAST_MODIFIED_ATTRIBUTE_NAME = "lastModified"

        fun createCachedCloudFile(result: SuccessfulDownloadResult): CachedFile {
            return try {
                AudioFileParser(result.cachedCloudFile).parse()
            } catch (ioe: InvalidBeatPrompterFileException) {
                try {
                    ImageFileParser(result.cachedCloudFile).parse()
                } catch (exception1: InvalidBeatPrompterFileException) {
                    try {
                        MIDIAliasFileParser(result.cachedCloudFile).parse()
                    } catch (exception2: InvalidBeatPrompterFileException) {
                        try {
                            SongInfoParser(result.cachedCloudFile).parse()
                        } catch (exception3: InvalidBeatPrompterFileException) {
                            try {
                                SetListFileParser(result.cachedCloudFile).parse()
                            } catch (exception4: InvalidBeatPrompterFileException) {
                                IrrelevantFile(result.cachedCloudFile)
                            }
                        }
                    }
                }
            }
        }
    }
}