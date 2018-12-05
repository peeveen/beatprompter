package com.stevenfrew.beatprompter.cache

@CacheXmlTag("imagefile")
/**
 * An image file from our cache.
 */
class ImageFile internal constructor(cachedCloudFileDescriptor: CachedFileDescriptor,
                                     val mWidth: Int,
                                     val mHeight: Int)
    : CachedFile(cachedCloudFileDescriptor)