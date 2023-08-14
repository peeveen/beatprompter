package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.set.SetListEntry

@CacheXmlTag("set")
/**
 * A set list file in our cache.
 */
class SetListFile internal constructor(
	cachedFile: CachedFile,
	val mSetTitle: String,
	val mSetListEntries: MutableList<SetListEntry>,
	errors: List<FileParseError>
) : CachedTextFile(cachedFile, errors)