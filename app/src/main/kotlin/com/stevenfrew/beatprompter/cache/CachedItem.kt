package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.util.normalize
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * A description of a cached storage item. Subclass of files and folders.
 */
abstract class CachedItem {
	val mID: String
	val mName: String
	val mNormalizedName: String
	var mSubfolderIDs: List<String>

	constructor(
		id: String,
		name: String,
		subfolderIDs: List<String>
	) {
		mID = id
		mName = name
		mNormalizedName = name.normalize()
		mSubfolderIDs = subfolderIDs
	}

	constructor(element: Element) {
		mID = element.getAttribute(CACHED_ITEM_ID_ATTRIBUTE_NAME)
		mName = element.getAttribute(CACHED_ITEM_NAME_ATTRIBUTE_NAME)
		mNormalizedName = mName.normalize()
		val elements = element.getElementsByTagName(CACHED_ITEM_SUBFOLDER_ELEMENT_NAME)
		val subfolderIDs = mutableListOf<String>()
		repeat(elements.length) {
			val subfolderElement = elements.item(it) as Element
			subfolderIDs.add(subfolderElement.textContent.trim())
		}
		mSubfolderIDs = subfolderIDs
	}

	open fun writeToXML(doc: Document, element: Element) {
		element.apply {
			setAttribute(CACHED_ITEM_NAME_ATTRIBUTE_NAME, mName)
			setAttribute(CACHED_ITEM_ID_ATTRIBUTE_NAME, mID)
		}
		mSubfolderIDs.forEach {
			val subfolderElement = doc.createElement(CACHED_ITEM_SUBFOLDER_ELEMENT_NAME)
			subfolderElement.textContent = it
			element.appendChild(subfolderElement)
		}
	}

	fun isInSubfolder(subfolder: String?): Boolean {
		if (subfolder.isNullOrBlank() || mSubfolderIDs.isEmpty())
			return subfolder.isNullOrBlank() && mSubfolderIDs.isEmpty()
		return mSubfolderIDs.contains(subfolder)
	}

	companion object {
		private const val CACHED_ITEM_ID_ATTRIBUTE_NAME = "id"
		private const val CACHED_ITEM_NAME_ATTRIBUTE_NAME = "name"
		private const val CACHED_ITEM_SUBFOLDER_ELEMENT_NAME = "subfolder"
	}
}