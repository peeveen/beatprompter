package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.*

@CacheXmlTag("set")
class SetListFile internal constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor, val mSetTitle:String, val mSongTitles:MutableList<String>, errors: List<FileParseError>) : CachedCloudTextFile(cachedCloudFileDescriptor,errors)