package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.Cache
import com.stevenfrew.beatprompter.cache.parse.tag.MalformedTagException
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.TagParsingUtility
import com.stevenfrew.beatprompter.cache.parse.tag.TagType
import com.stevenfrew.beatprompter.cache.parse.tag.find.Type
import com.stevenfrew.beatprompter.comm.midi.message.Message
import com.stevenfrew.beatprompter.comm.midi.message.OutgoingMessage
import com.stevenfrew.beatprompter.midi.EventOffset
import com.stevenfrew.beatprompter.midi.EventOffsetType
import com.stevenfrew.beatprompter.midi.alias.Alias
import com.stevenfrew.beatprompter.midi.alias.ChannelValue
import com.stevenfrew.beatprompter.midi.alias.ResolutionException
import com.stevenfrew.beatprompter.midi.alias.Value
import com.stevenfrew.beatprompter.song.event.MIDIEvent
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
	val mMessages: List<OutgoingMessage>
	val mOffset: EventOffset

	init {
		val parsedEvent = parseMIDIEvent(
			name,
			value, lineNumber, Cache.mCachedCloudItems.midiAliases
		)
		mMessages = parsedEvent.first
		mOffset = parsedEvent.second
	}

	fun toMIDIEvent(time: Long): MIDIEvent {
		return MIDIEvent(time, mMessages, mOffset)
	}

	companion object {
		fun parseMIDIEvent(
			name: String,
			value: String,
			lineNumber: Int,
			aliases: List<Alias>
		): Pair<List<OutgoingMessage>, EventOffset> {
			val defaultChannel = Message.getChannelFromBitmask(Preferences.defaultMIDIOutputChannel)

			val (tagName, tagValue, eventOffset) = normalizeMIDIValues(name, value, lineNumber)

			val firstPassParamValues =
				tagValue
					.splitAndTrim(",")
					.asSequence()
					.filter { it.isNotBlank() }
					.map { bit -> parseValue(bit) }
					.toList()

			val (params, channelValue) =
				separateParametersFromChannel(firstPassParamValues, defaultChannel)

			try {
				val resolvedBytes = params
					.map { it.resolve() }
					.toByteArray()
				val matchedAlias = aliases.firstOrNull {
					it.mName.equals(
						tagName,
						ignoreCase = true
					) && it.parameterCount == resolvedBytes.size
				}
				return when {
					tagName == "midi_send" -> listOf(OutgoingMessage(resolvedBytes))
					matchedAlias != null -> matchedAlias.resolve(
						aliases,
						resolvedBytes,
						channelValue.resolve()
					)

					else -> throw MalformedTagException(R.string.unknown_midi_directive, tagName)
				} to eventOffset
			} catch (re: ResolutionException) {
				throw MalformedTagException(re.message!!)
			}
		}

		private fun separateParametersFromChannel(
			params: List<Value>,
			defaultChannel: Byte
		): Pair<List<Value>, ChannelValue> {
			val channelValue = params.mapIndexed { index, param ->
				if (param is ChannelValue && index != params.size - 1)
					throw MalformedTagException(R.string.channel_must_be_last_parameter)
				param
			}.filterIsInstance<ChannelValue>().firstOrNull()
			return Pair(
				if (channelValue == null) params else params.dropLast(1), channelValue
					?: ChannelValue(defaultChannel)
			)
		}

		private fun normalizeMIDIValues(
			name: String,
			value: String,
			lineNumber: Int
		): Triple<String, String, EventOffset> {
			return if (value.isEmpty()) {
				// A MIDI tag of {blah;+33} ends up with "blah;+33" as the tag name. Fix it here.
				if (name.contains(";")) {
					val bits = name.splitAndTrim(";")
					if (bits.size > 2)
						throw MalformedTagException(R.string.multiple_semi_colons_in_midi_tag)
					if (bits.size > 1)
						Triple(bits[0], value, parseMIDIEventOffset(bits[1], lineNumber))
					else
						Triple(name, value, EventOffset(lineNumber))
				} else
					Triple(name, value, EventOffset(lineNumber))
			} else {
				val firstSplitBits = value.splitAndTrim(";")
				if (firstSplitBits.size > 1) {
					if (firstSplitBits.size > 2)
						throw MalformedTagException(R.string.multiple_semi_colons_in_midi_tag)
					Triple(name, firstSplitBits[0], parseMIDIEventOffset(firstSplitBits[1], lineNumber))
				} else
					Triple(name, value, EventOffset(lineNumber))
			}
		}

		private fun parseMIDIEventOffset(offsetString: String, lineNumber: Int): EventOffset {
			val trimmedOffsetString = offsetString.trim()
			if (trimmedOffsetString.isNotEmpty()) {
				try {
					return try {
						EventOffset(
							trimmedOffsetString.toInt(),
							EventOffsetType.Milliseconds, lineNumber
						)
					} catch (e: NumberFormatException) {
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
			}
			return EventOffset(lineNumber)
		}

		private fun parseValue(strVal: String): Value {
			return TagParsingUtility.parseMIDIValue(strVal, 0, 1)
		}
	}
}
