package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.set.SetListEntry
import com.stevenfrew.beatprompter.cache.parse.*

@CacheXmlTag("set")
/**
 * A set list file in our cache.
 */
class SetListFile internal constructor(cachedCloudFileDescriptor: CachedFileDescriptor, val mSetTitle: String, val mSetListEntries: MutableList<SetListEntry>, errors: List<FileParseError>) : CachedTextFile(cachedCloudFileDescriptor, errors)