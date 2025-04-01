package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.ContentParsingError
import com.stevenfrew.beatprompter.set.SetListEntry

@CacheXmlTag("set")
/**
 * A set list file in our cache.
 */
class SetListFile internal constructor(
	cachedFile: CachedFile,
	val setTitle: String,
	val setListEntries: MutableList<SetListEntry>,
	errors: List<ContentParsingError>
) : CachedTextFile(cachedFile, errors)