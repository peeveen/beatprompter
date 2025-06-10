package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.Cache
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.comm.midi.message.MidiMessage
import com.stevenfrew.beatprompter.midi.EventOffset
import com.stevenfrew.beatprompter.midi.EventOffsetType
import com.stevenfrew.beatprompter.midi.alias.AliasSet
import com.stevenfrew.beatprompter.midi.alias.ChannelValue
import com.stevenfrew.beatprompter.midi.alias.ResolutionException
import com.stevenfrew.beatprompter.midi.alias.Value
import com.stevenfrew.beatprompter.song.event.MidiEvent
import com.stevenfrew.beatprompter.util.splitAndTrim

@TagType(Type.Directive)
/**
 * Tag that defines a MIDI event to be output to any connected devices.
 */
class MidiEventTag internal constructor(
	name: String,
	lineNumber: Int,
	position: Int,
	value: String
) : Tag(name, lineNumber, position) {
	val messages: List<MidiMessage>
	val offset: EventOffset
	val aliasSetsUsed: Set<AliasSet>

	init {
		val parsedEvent = parseMIDIEvent(
			name,
			value, lineNumber, Cache.cachedCloudItems.midiAliasSets
		)
		messages = parsedEvent.first.first
		offset = parsedEvent.second
		aliasSetsUsed = parsedEvent.first.second
	}

	fun toMIDIEvent(time: Long): MidiEvent = MidiEvent(time, messages, offset)

	companion object {
		const val MIDI_SEND_TAG = "midi_send"

		fun parseMIDIEvent(
			name: String,
			value: String,
			lineNumber: Int,
			aliasSets: List<AliasSet>
		): Pair<Pair<List<MidiMessage>, Set<AliasSet>>, EventOffset> =
			normalizeMIDIValues(name, value, lineNumber).let { (tagName, tagValue, eventOffset) ->
				val firstPassParamValues =
					tagValue
						.splitAndTrim(",")
						.asSequence()
						.filter { it.isNotBlank() }
						.map { bit -> parseValue(bit) }
						.toList()

				val (params, channelValue) =
					separateParametersFromChannel(
						firstPassParamValues,
						MidiMessage.getChannelFromBitmask(BeatPrompter.preferences.defaultMIDIOutputChannel)
					)

				try {
					val resolvedBytes = params
						.map { it.resolve() }
						.toByteArray()
					val matchedAliasAndSet = aliasSets.firstNotNullOfOrNull { set ->
						set.aliases.firstOrNull {
							it.name.equals(
								tagName,
								ignoreCase = true
							) && it.parameterCount == resolvedBytes.size
						}?.let {
							it to set
						}
					}
					when {
						tagName == MIDI_SEND_TAG -> listOf(MidiMessage(resolvedBytes)) to setOf(Cache.cachedCloudItems.defaultMidiAliasSet)
						matchedAliasAndSet != null -> matchedAliasAndSet.first.resolve(
							matchedAliasAndSet.second,
							aliasSets,
							resolvedBytes,
							channelValue.resolve()
						)

						else -> throw MalformedTagException(R.string.unknown_midi_directive, tagName)
					} to eventOffset
				} catch (re: ResolutionException) {
					throw MalformedTagException(re.message)
				}
			}

		private fun separateParametersFromChannel(
			params: List<Value>,
			defaultChannel: Byte
		): Pair<List<Value>, ChannelValue> =
			// Ensure channel is last parameter.
			params.mapIndexed { index, param ->
				if (param is ChannelValue && index != params.size - 1)
					throw MalformedTagException(R.string.channel_must_be_last_parameter)
				param
			}.filterIsInstance<ChannelValue>().firstOrNull().let { channelValue ->
				Pair(
					if (channelValue == null) params else params.dropLast(1),
					channelValue ?: ChannelValue(defaultChannel)
				)
			}

		private fun normalizeMIDIValues(
			name: String,
			value: String,
			lineNumber: Int
		): Triple<String, String, EventOffset> =
			if (value.isEmpty())
				if (name.contains(";")) // A MIDI tag of {blah;+33} ends up with "blah;+33" as the tag name. Fix it here.
					parseSemiColonDataFromTag(
						name,
						name,
						value,
						lineNumber,
						{ bits -> bits[0] },
						{ _ -> value }
					)
				else
					Triple(name, value, EventOffset(lineNumber))
			else
				parseSemiColonDataFromTag(
					value,
					name,
					value,
					lineNumber,
					{ _ -> name },
					{ bits -> bits[0] }
				)

		private fun parseSemiColonDataFromTag(
			toParse: String,
			name: String,
			value: String,
			lineNumber: Int,
			nameSelector: (List<String>) -> String,
			valueSelector: (List<String>) -> String
		): Triple<String, String, EventOffset> =
			toParse.splitAndTrim(";").takeUnless { it.size > 2 }?.let {
				if (it.size > 1)
					Triple(
						nameSelector(it),
						valueSelector(it),
						parseMIDIEventOffset(it[1], lineNumber)
					)
				else
					Triple(name, value, EventOffset(lineNumber))
			} ?: throw MalformedTagException(R.string.multiple_semi_colons_in_midi_tag)

		private fun parseMIDIEventOffset(offsetString: String, lineNumber: Int): EventOffset =
			offsetString.trim().takeUnless { it.isEmpty() }?.let { trimmedOffsetString ->
				try {
					try {
						EventOffset(
							trimmedOffsetString.toInt(),
							EventOffsetType.Milliseconds, lineNumber
						)
					} catch (_: NumberFormatException) {
						// Might be in the beat format
						val offsetChars = trimmedOffsetString.toCharArray()
						val upDiff = offsetChars.count { it == '>' }
						val downDiff = offsetChars.count { it == '<' }
						if (offsetChars.any { it != '<' && it != '>' })
							throw MalformedTagException(R.string.non_beat_characters_in_midi_offset)
						EventOffset(upDiff - downDiff, EventOffsetType.Beats, lineNumber)
					}
				} catch (badValueEx: IllegalArgumentException) {
					throw MalformedTagException(badValueEx.message!!)
				}
			} ?: EventOffset(lineNumber)

		private fun parseValue(strVal: String): Value = TagParsingUtility.parseMIDIValue(strVal, 0, 1)
	}
}
