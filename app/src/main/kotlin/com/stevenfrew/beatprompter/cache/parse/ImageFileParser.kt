package com.stevenfrew.beatprompter.cache.parse

import android.graphics.BitmapFactory
import android.util.Size
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.CachedFile
import com.stevenfrew.beatprompter.cache.ImageFile
import org.w3c.dom.Element

/**
 * "Parser" for image files. Basically validates that the file is actually an image.
 */
class ImageFileParser(cachedCloudFile: CachedFile) : FileParser<ImageFile>(cachedCloudFile) {
	override fun parse(element: Element?): ImageFile =
		try {
			ImageFile.readDimensionsFromAttributes(element)?.let {
				ImageFile(mCachedCloudFile, it)
			} ?: BitmapFactory.decodeFile(
				mCachedCloudFile.mFile.absolutePath,
				BitmapFactory.Options()
			).let {
				ImageFile(mCachedCloudFile, Size(it.width, it.height))
			}
		} catch (e: Exception) {
			// Not bothered about what the exception is. File is obviously shite.
			null
		} ?: throw InvalidBeatPrompterFileException(
			R.string.could_not_read_image_file,
			mCachedCloudFile.mName
		)
}
