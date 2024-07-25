package com.stevenfrew.beatprompter.cache

import android.util.Size
import org.w3c.dom.Document
import org.w3c.dom.Element

@CacheXmlTag("imagefile")
/**
 * An image file from our cache.
 */
class ImageFile internal constructor(
	cachedFile: CachedFile,
	val size: Size
) : CachedFile(cachedFile) {
	override fun writeToXML(doc: Document, element: Element) {
		super.writeToXML(doc, element)
		element.setAttribute(WIDTH_ATTRIBUTE, "${size.width}")
		element.setAttribute(HEIGHT_ATTRIBUTE, "${size.height}")
	}

	companion object {
		private const val WIDTH_ATTRIBUTE = "imageWidth"
		private const val HEIGHT_ATTRIBUTE = "imageHeight"

		fun readDimensionsFromAttributes(element: Element?): Size? =
			if (element?.hasAttribute(WIDTH_ATTRIBUTE) == true && element.hasAttribute(HEIGHT_ATTRIBUTE)) {
				val widthString = element.getAttribute(WIDTH_ATTRIBUTE)
				val heightString = element.getAttribute(HEIGHT_ATTRIBUTE)
				try {
					val width = widthString.toInt()
					val height = heightString.toInt()
					Size(width, height)
				} catch (numberFormatException: NumberFormatException) {
					// Attribute is garbage, we'll need to actually examine the file.
					null
				}
			} else null
	}
}
