package com.stevenfrew.beatprompter.cache

@CacheXmlTag("audiofile")
/**
 * An audio file from our cache of files.
 */
class AudioFile internal constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor, val mDuration: Long) : CachedCloudFile(cachedCloudFileDescriptor)