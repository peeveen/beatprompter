package com.stevenfrew.beatprompter.cache.parse

import android.graphics.BitmapFactory
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedFileDescriptor
import com.stevenfrew.beatprompter.cache.ImageFile

/**
 * "Parser" for image files. Basically validates that the file is actually an image.
 */
class ImageFileParser(cachedCloudFileDescriptor: CachedFileDescriptor)
    : FileParser<ImageFile>(cachedCloudFileDescriptor) {

    override fun parse(): ImageFile {
        try {
            BitmapFactory.decodeFile(mCachedCloudFileDescriptor.mFile.absolutePath,
                    BitmapFactory.Options())?.also {
                return ImageFile(mCachedCloudFileDescriptor, it.width, it.height)
            }
        } catch (e: Exception) {
            // Not bothered about what the exception is. File is obviously shite.
        }
        throw InvalidBeatPrompterFileException(R.string.could_not_read_image_file, mCachedCloudFileDescriptor.mName)
    }
}