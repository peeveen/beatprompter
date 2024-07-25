package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.util.normalize
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * A description of a cached storage item. Subclass of files and folders.
 */
abstract class CachedItem {
	val id: String
	val name: String
	val normalizedName: String
	var subfolderIds: List<String>

	constructor(
		id: String,
		name: String,
		subfolderIDs: List<String>
	) {
		this.id = id
		this.name = name
		normalizedName = name.normalize()
		subfolderIds = subfolderIDs
	}

	constructor(element: Element) {
		id = element.getAttribute(CACHED_ITEM_ID_ATTRIBUTE_NAME)
		name = element.getAttribute(CACHED_ITEM_NAME_ATTRIBUTE_NAME)
		normalizedName = name.normalize()
		val elements = element.getElementsByTagName(CACHED_ITEM_SUBFOLDER_ELEMENT_NAME)
		val subfolderIDs = mutableListOf<String>()
		repeat(elements.length) {
			val subfolderElement = elements.item(it) as Element
			subfolderIDs.add(subfolderElement.textContent.trim())
		}
		subfolderIds = subfolderIDs
	}

	open fun writeToXML(doc: Document, element: Element) =
		element.run {
			setAttribute(CACHED_ITEM_NAME_ATTRIBUTE_NAME, name)
			setAttribute(CACHED_ITEM_ID_ATTRIBUTE_NAME, id)
			subfolderIds.forEach {
				val subfolderElement = doc.createElement(CACHED_ITEM_SUBFOLDER_ELEMENT_NAME)
				subfolderElement.textContent = it
				appendChild(subfolderElement)
			}
		}

	fun isInSubfolder(subfolder: String?): Boolean =
		(subfolder.isNullOrBlank() && subfolderIds.isEmpty()) || subfolderIds.contains(subfolder)

	companion object {
		private const val CACHED_ITEM_ID_ATTRIBUTE_NAME = "id"
		private const val CACHED_ITEM_NAME_ATTRIBUTE_NAME = "name"
		private const val CACHED_ITEM_SUBFOLDER_ELEMENT_NAME = "subfolder"
	}
}