package com.stevenfrew.beatprompter.cache

@CacheXmlTag("audiofile")
class AudioFile internal constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor, val mDurationMilliseconds:Int) : CachedCloudFile(cachedCloudFileDescriptor)