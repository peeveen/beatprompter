package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.midi.SongTrigger
import com.stevenfrew.beatprompter.midi.TriggerType
import com.stevenfrew.beatprompter.midi.alias.ChannelValue
import com.stevenfrew.beatprompter.midi.alias.WildcardValue
import com.stevenfrew.beatprompter.util.splitAndTrim

/**
 * Base class for MIDI auto-start tags.
 */
open class MidiTriggerTag protected constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	triggerDescriptor: String,
	type: TriggerType
) : Tag(name, lineNumber, position) {
	val mTrigger: SongTrigger

	init {
		val bits = triggerDescriptor.splitAndTrim(",")
		if (bits.size > 1)
			if (type == TriggerType.SongSelect)
				throw MalformedTagException(R.string.song_index_must_have_one_value)
		if (bits.size > 4 || bits.isEmpty())
			if (type == TriggerType.SongSelect)
				throw MalformedTagException(R.string.song_index_must_have_one_value)
			else
				throw MalformedTagException(R.string.song_index_must_have_one_two_or_three_values)

		val channel = if (bits.size > 3) {
			val value = TagParsingUtility.parseMIDIValue(bits[3], 3, bits.size)
			if (value is ChannelValue)
				value
			else
				WildcardValue()
		} else
			WildcardValue()

		val lsb =
			if (bits.size > 2)
				TagParsingUtility.parseMIDIValue(bits[2], 2, bits.size)
			else
				WildcardValue()
		val msb =
			if (bits.size > 1)
				TagParsingUtility.parseMIDIValue(bits[1], 1, bits.size)
			else
				WildcardValue()
		val index = TagParsingUtility.parseMIDIValue(bits[0], 0, bits.size)

		mTrigger = SongTrigger(msb, lsb, index, channel, type)
	}
}