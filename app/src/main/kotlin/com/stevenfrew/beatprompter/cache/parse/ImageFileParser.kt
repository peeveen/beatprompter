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
class ImageFileParser(private val cachedCloudFile: CachedFile) : ContentParser<ImageFile>() {
	override fun parse(element: Element?): ImageFile =
		try {
			ImageFile.readDimensionsFromAttributes(element)?.let {
				ImageFile(cachedCloudFile, it)
			} ?: BitmapFactory.decodeFile(
				cachedCloudFile.file.absolutePath,
				BitmapFactory.Options()
			).let {
				ImageFile(cachedCloudFile, Size(it.width, it.height))
			}
		} catch (_: Exception) {
			// Not bothered about what the exception is. File is obviously shite.
			null
		} ?: throw InvalidBeatPrompterFileException(
			R.string.could_not_read_image_file,
			cachedCloudFile.name
		)
}
