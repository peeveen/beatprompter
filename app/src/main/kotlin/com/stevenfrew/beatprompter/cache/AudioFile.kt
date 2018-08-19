package com.stevenfrew.beatprompter.cache

@CacheXmlTag("audiofile")
class AudioFile internal constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor) : CachedCloudFile(cachedCloudFileDescriptor)