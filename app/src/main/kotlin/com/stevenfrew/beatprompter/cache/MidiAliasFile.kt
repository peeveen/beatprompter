package com.stevenfrew.beatprompter.cache

import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.midi.alias.AliasSet

@CacheXmlTag("midialiases")
/**
 * A MIDI alias file in our cache.
 */
class MidiAliasFile internal constructor(
	cachedFile: CachedFile,
	val aliasSet: AliasSet,
	errors: List<FileParseError>
) : CachedTextFile(cachedFile, errors)