package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.Cache
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.OncePerFile
import com.stevenfrew.beatprompter.cache.parse.tag.TagName
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.ValueTag
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.midi.alias.AliasSet
import com.stevenfrew.beatprompter.util.splitAndTrim

@OncePerFile
@TagName("activate_midi_aliases")
@TagType(Type.Directive)
/**
 * Tag that activates a MIDI alias set.
 */
class ActivateMidiAliasesTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : ValueTag(name, lineNumber, position, value) {
	val midiAliasSets: Set<AliasSet>

	init {
		val midiAliasSetNames = value.splitAndTrim(",")
		midiAliasSets =
			midiAliasSetNames
				.map { aliasSetName ->
					Cache.cachedCloudItems.midiAliasSets.firstOrNull { it.name == aliasSetName }
						?: throw MalformedTagException(
							BeatPrompter.appResources.getString(
								R.string.no_such_midi_alias_set,
								aliasSetName
							)
						)
				}
				.toSet()
	}
}