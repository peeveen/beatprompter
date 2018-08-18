package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.FileParseError

abstract class CachedCloudTextFile internal constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor,val mErrors:List<FileParseError>):CachedCloudFile(cachedCloudFileDescriptor)