package com.stevenfrew.beatprompter.cache.parse.tag

import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.TextContentParser
import com.stevenfrew.beatprompter.midi.alias.ArgumentValue
import com.stevenfrew.beatprompter.midi.alias.ChannelValue
import com.stevenfrew.beatprompter.midi.alias.ChanneledCommandValue
import com.stevenfrew.beatprompter.midi.alias.CommandValue
import com.stevenfrew.beatprompter.midi.alias.ComparisonType
import com.stevenfrew.beatprompter.midi.alias.ComparisonValue
import com.stevenfrew.beatprompter.midi.alias.NoValue
import com.stevenfrew.beatprompter.midi.alias.Value
import com.stevenfrew.beatprompter.midi.alias.ValueException
import com.stevenfrew.beatprompter.midi.alias.WildcardValue
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.looksLikeDecimal
import com.stevenfrew.beatprompter.util.looksLikeHex
import kotlin.reflect.KClass

/**
 * Singleton map of parser-type to TagParsingHelper. Saves a lot of annotation processing.
 * Should only construct one TagParsingHelper per file type, instead of one per file.
 */
object TagParsingUtility {
	private val helperMap = mutableMapOf<KClass<out Any>, TagParsingHelper<Any>>()

	@Suppress("UNCHECKED_CAST")
	fun <T> getTagParsingHelper(parser: TextContentParser<T>): TagParsingHelper<T> =
		helperMap.getOrPut(parser::class) {
			TagParsingHelper(parser) as TagParsingHelper<Any>
		} as TagParsingHelper<T>

	fun parseIntegerValue(value: String, min: Int, max: Int): Int =
		try {
			value.toInt().also {
				if (it < min)
					throw MalformedTagException(R.string.intValueTooLow, min, it)
				else if (it > max)
					throw MalformedTagException(R.string.intValueTooHigh, max, it)
			}
		} catch (_: NumberFormatException) {
			throw MalformedTagException(R.string.intValueUnreadable, value)
		}

	fun parseDurationValue(value: String, min: Long, max: Long, trackLengthAllowed: Boolean): Long =
		try {
			Utils.milliToNano(Utils.parseDuration(value, trackLengthAllowed).also {
				if (it < min && it != Utils.TRACK_AUDIO_LENGTH_VALUE)
					throw MalformedTagException(
						BeatPrompter.appResources.getString(
							R.string.intValueTooLow,
							min,
							it
						)
					)
				else if (it > max)
					throw MalformedTagException(
						BeatPrompter.appResources.getString(
							R.string.intValueTooHigh,
							max,
							it
						)
					)
			})
		} catch (_: NumberFormatException) {
			throw MalformedTagException(
				BeatPrompter.appResources.getString(
					R.string.durationValueUnreadable,
					value
				)
			)
		}

	fun parseDoubleValue(value: String, min: Int, max: Int): Double =
		try {
			value.toDouble().also {
				if (it < min)
					throw MalformedTagException(
						BeatPrompter.appResources.getString(
							R.string.doubleValueTooLow,
							min,
							it
						)
					)
				else if (it > max)
					throw MalformedTagException(
						BeatPrompter.appResources.getString(
							R.string.doubleValueTooHigh,
							max,
							it
						)
					)
			}
		} catch (_: NumberFormatException) {
			throw MalformedTagException(
				BeatPrompter.appResources.getString(
					R.string.doubleValueUnreadable,
					value
				)
			)
		}

	fun parseColourValue(value: String): Int =
		try {
			BeatPrompter.platformUtils.parseColor(value)
		} catch (_: IllegalArgumentException) {
			try {
				BeatPrompter.platformUtils.parseColor("#$value")
			} catch (_: IllegalArgumentException) {
				throw MalformedTagException(
					BeatPrompter.appResources.getString(
						R.string.colorValueUnreadable,
						value
					)
				)
			}
		}

	private fun parseByteValue(value: String): Byte =
		when {
			value.looksLikeHex() -> Utils.parseHexByte(value)
			value.looksLikeDecimal() -> Utils.parseByte(value)
			else -> throw MalformedTagException(BeatPrompter.appResources.getString(R.string.not_a_valid_byte_value))
		}

	fun parseMIDIValue(valueStr: String, argIndex: Int, argCount: Int): Value =
		valueStr.trim().let {
			if (it.isEmpty())
				NoValue()
			try {
				when {
					it == "*" -> return WildcardValue()
					it.startsWith("?") -> {
						val withoutQuestion = it.substring(1)
						try {
							// Arguments are one-based in the alias files.
							ArgumentValue(Integer.parseInt(withoutQuestion) - 1)
						} catch (_: NumberFormatException) {
							throw MalformedTagException(BeatPrompter.appResources.getString(R.string.not_a_valid_argument_index))
						}
					}

					it.startsWith("#") -> {
						if (argIndex < argCount - 1)
							throw MalformedTagException(BeatPrompter.appResources.getString(R.string.channel_must_be_last_parameter))
						// Channel is 1-based in text, but 0-based in code.
						ChannelValue((Utils.parseByte(it.substring(1)) - 1).toByte())
					}

					it.contains("_") -> {
						if (it.indexOf("_") != it.lastIndexOf("_"))
							throw MalformedTagException(BeatPrompter.appResources.getString(R.string.multiple_underscores_in_midi_value))
						val zeroedValue = it.replace('_', '0')
						if (zeroedValue.looksLikeHex()) {
							ChanneledCommandValue(Utils.parseHexByte(zeroedValue))
						} else
							throw MalformedTagException(BeatPrompter.appResources.getString(R.string.underscore_in_decimal_value))
					}

					// Order is IMPORTANT here!
					it.startsWith("<=") -> ComparisonValue(
						ComparisonType.LessThanOrEqualTo,
						parseByteValue(it.substring(2))
					)

					it.startsWith(">=") -> ComparisonValue(
						ComparisonType.GreaterThanOrEqualTo,
						parseByteValue(it.substring(2))
					)

					it.startsWith("<") -> ComparisonValue(
						ComparisonType.LessThan,
						parseByteValue(it.substring(1))
					)

					it.startsWith(">") -> ComparisonValue(
						ComparisonType.GreaterThan,
						parseByteValue(it.substring(1))
					)

					else -> CommandValue(parseByteValue(it))
				}
			} catch (valueEx: ValueException) {
				throw MalformedTagException(valueEx.message!!)
			} catch (_: NumberFormatException) {
				throw MalformedTagException(BeatPrompter.appResources.getString(R.string.not_a_valid_byte_value))
			}
		}
}