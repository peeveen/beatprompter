package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.*
import com.stevenfrew.beatprompter.midi.*

@CacheXmlTag("midialiases")
class MIDIAliasFile internal constructor(cachedCloudFileDescriptor: CachedCloudFileDescriptor, val mAliasSet: AliasSet, errors: List<FileParseError>) : CachedCloudTextFile(cachedCloudFileDescriptor,errors)