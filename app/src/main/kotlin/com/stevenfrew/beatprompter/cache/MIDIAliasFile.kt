package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.*
import com.stevenfrew.beatprompter.midi.alias.AliasSet

@CacheXmlTag("midialiases")
/**
 * A MIDI alias file in our cache.
 */
class MIDIAliasFile internal constructor(cachedCloudFileDescriptor: CachedFileDescriptor, val mAliasSet: AliasSet, errors: List<FileParseError>) : CachedTextFile(cachedCloudFileDescriptor, errors)