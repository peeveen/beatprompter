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
            BitmapFactory.decodeFile(mCachedCloudFileDescriptor.mFile.absolutePath, options)
                    ?: throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.could_not_read_image_file) + ": " + mCachedCloudFileDescriptor.mName)
        } catch (e: Exception) {
            throw InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.could_not_read_image_file) + ": " + mCachedCloudFileDescriptor.mName)
        }
        return ImageFile(mCachedCloudFileDescriptor)
    }
}