package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.*
import com.stevenfrew.beatprompter.cloud.CloudFileInfo
import com.stevenfrew.beatprompter.cloud.SuccessfulCloudDownloadResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.util.*

abstract class CachedCloudFile : CachedCloudFileDescriptor {
    internal constructor(file: File, id: String, name: String, lastModified: Date, subfolder: String?):super(file,id,name,lastModified,subfolder)

    internal constructor(file: File, cloudFileInfo: CloudFileInfo) : this(file,cloudFileInfo.mID, cloudFileInfo.mName, cloudFileInfo.mLastModified, cloudFileInfo.mSubfolder)

    internal constructor(result: SuccessfulCloudDownloadResult) : this(result.mDownloadedFile, result.mCloudFileInfo)

    internal constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor) : this(cachedCloudFileDescriptor.mFile,cachedCloudFileDescriptor)

    internal constructor(element: Element) : super(element)

    abstract fun writeToXML(d: Document, element: Element)

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
                            SongInfoParser(result.cachedCloudFileDescriptor,listOf(),listOf()).parse()
                        } catch (ibpfe3: InvalidBeatPrompterFileException) {
                            try {
                                SetListFileParser(result.cachedCloudFileDescriptor).parse()
                            } catch (ibpfe4: InvalidBeatPrompterFileException) {
                                IrrelevantFile(result)
                            }
                        }
                    }
                }
            }
        }
    }
}