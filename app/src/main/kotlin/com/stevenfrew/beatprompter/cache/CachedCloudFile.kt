package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.*
import com.stevenfrew.beatprompter.cloud.SuccessfulCloudDownloadResult
import org.w3c.dom.Document
import org.w3c.dom.Element

abstract class CachedCloudFile internal constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor) : CachedCloudFileDescriptor(cachedCloudFileDescriptor.mFile, cachedCloudFileDescriptor.mID, cachedCloudFileDescriptor.mName, cachedCloudFileDescriptor.mLastModified, cachedCloudFileDescriptor.mSubfolder) {

    fun writeToXML(d: Document, element: Element)
    {
        val newElement = d.createElement(this::class.annotations.filterIsInstance<CacheXmlTag>().first().mTag)
        super.writeToXML(newElement)
        element.appendChild(newElement)
    }

    companion object {

        fun createCachedCloudFile(result: SuccessfulCloudDownloadResult): CachedCloudFile {
            return try {
                AudioFileParser(result.cachedCloudFileDescriptor).parse()
            } catch (ioe: InvalidBeatPrompterFileException) {
                try {
                    ImageFileParser(result.cachedCloudFileDescriptor).parse()
                } catch (ibpfe1: InvalidBeatPrompterFileException) {
                    try {
                        MIDIAliasFileParser(result.cachedCloudFileDescriptor).parse()
                    } catch (ibpfe2: InvalidBeatPrompterFileException) {
                        try {
                            SongInfoParser(result.cachedCloudFileDescriptor).parse()
                        } catch (ibpfe3: InvalidBeatPrompterFileException) {
                            try {
                                SetListFileParser(result.cachedCloudFileDescriptor).parse()
                            } catch (ibpfe4: InvalidBeatPrompterFileException) {
                                IrrelevantFile(result.cachedCloudFileDescriptor)
                            }
                        }
                    }
                }
            }
        }
    }
}