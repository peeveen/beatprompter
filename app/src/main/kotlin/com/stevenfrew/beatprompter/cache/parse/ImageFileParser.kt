package com.stevenfrew.beatprompter.cache.parse

import android.graphics.BitmapFactory
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedCloudFileDescriptor
import com.stevenfrew.beatprompter.cache.ImageFile

class ImageFileParser constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor):FileParser<ImageFile>(cachedCloudFileDescriptor) {

    override fun parse(): ImageFile {
        val options = BitmapFactory.Options()
        try {
            val bitmap=BitmapFactory.decodeFile(mCachedCloudFileDescriptor.mFile.absolutePath, options)
                    ?: throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.could_not_read_image_file) + ": " + mCachedCloudFileDescriptor.mName)
            return ImageFile(mCachedCloudFileDescriptor,bitmap.width,bitmap.height)
        } catch (e: Exception) {
            // Not bothered about what the exception is. File is obviously shite.
        }
        throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.could_not_read_image_file) + ": " + mCachedCloudFileDescriptor.mName)
    }
}