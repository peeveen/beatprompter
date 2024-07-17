package com.stevenfrew.beatprompter.cache

import org.w3c.dom.Element

@CacheXmlTag("folder")
/**
 * A description of a storage folder. Folders are NEVER recreated locally, but we keep track of them
 * for filtering purposes.
 */
class CachedFolder : CachedItem {
	constructor(
		id: String,
		name: String,
		subfolderIDs: List<String>
	) : super(id, name, subfolderIDs)

	constructor(element: Element) : super(element)
}