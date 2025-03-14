package com.stevenfrew.beatprompter.cache.parse.tag.midi.trigger

import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.midi.CommandTrigger
import com.stevenfrew.beatprompter.midi.MidiTrigger
import com.stevenfrew.beatprompter.midi.SongTrigger
import com.stevenfrew.beatprompter.midi.TriggerType
import com.stevenfrew.beatprompter.midi.alias.ChannelValue
import com.stevenfrew.beatprompter.midi.alias.WildcardValue
import com.stevenfrew.beatprompter.util.splitAndTrim

/**
 * Base class for MIDI trigger tags.
 */
open class MidiTriggerTag protected constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	triggerDescriptor: String,
	type: TriggerType
) : Tag(name, lineNumber, position) {
	val trigger: MidiTrigger

	init {
		val parseData = when (type) {
			TriggerType.SongSelect -> intArrayOf(1, 1, -1, R.string.song_select_must_have_one_value)
			TriggerType.ProgramChange -> intArrayOf(
				1,
				4,
				3,
				R.string.program_change_must_have_one_to_four_values
			)

			TriggerType.ControlChange -> intArrayOf(
				2,
				3,
				2,
				R.string.control_change_must_have_two_or_three_values
			)
		}

		val minLength = parseData[0]
		val maxLength = parseData[1]
		val channelIndex = parseData[2]
		val errorId = parseData[3]

		val bits = triggerDescriptor.splitAndTrim(",")
		if (bits.size < minLength || bits.size > maxLength)
			throw MalformedTagException(errorId)
		val channel = if (channelIndex != -1 && bits.size > channelIndex) {
			val value = TagParsingUtility.parseMIDIValue(bits[channelIndex], channelIndex, bits.size)
			value as? ChannelValue ?: WildcardValue()
		} else
			WildcardValue()

		trigger = if (type == TriggerType.ControlChange) {
			val controller =
				if (bits.isNotEmpty())
					TagParsingUtility.parseMIDIValue(bits[0], 0, bits.size)
				else
					WildcardValue()
			val value =
				if (bits.size > 1)
					TagParsingUtility.parseMIDIValue(bits[1], 1, bits.size)
				else
					WildcardValue()
			CommandTrigger(controller, value, channel)
		} else {
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
			SongTrigger(msb, lsb, index, channel, type)
		}
	}
}