package com.stevenfrew.beatprompter.cache

@CacheXmlTag("imagefile")
class ImageFile internal constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor,val mWidth:Int,val mHeight:Int) : CachedCloudFile(cachedCloudFileDescriptor)