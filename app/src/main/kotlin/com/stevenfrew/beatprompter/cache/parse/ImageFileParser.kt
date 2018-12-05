package com.stevenfrew.beatprompter.cache.parse

import android.graphics.BitmapFactory
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedFileDescriptor
import com.stevenfrew.beatprompter.cache.ImageFile

/**
 * "Parser" for image files. Basically validates that the file is actually an image.
 */
class ImageFileParser constructor(cachedCloudFileDescriptor: CachedFileDescriptor) : FileParser<ImageFile>(cachedCloudFileDescriptor) {

    override fun parse(): ImageFile {
        val options = BitmapFactory.Options()
        try {
            val bitmap = BitmapFactory.decodeFile(mCachedCloudFileDescriptor.mFile.absolutePath, options)
            if (bitmap != null)
                return ImageFile(mCachedCloudFileDescriptor, bitmap.width, bitmap.height)
        } catch (e: Exception) {
            // Not bothered about what the exception is. File is obviously shite.
        }
        throw InvalidBeatPrompterFileException(R.string.could_not_read_image_file, mCachedCloudFileDescriptor.mName)
    }
}