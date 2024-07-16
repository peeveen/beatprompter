package com.stevenfrew.beatprompter.cache.parse.tag

import android.graphics.Color
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.parse.TextFileParser
import com.stevenfrew.beatprompter.midi.alias.*
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.looksLikeDecimal
import com.stevenfrew.beatprompter.util.looksLikeHex
import kotlin.reflect.KClass

/**
 * Singleton map of parser-type to TagParsingHelper. Saves a lot of annotation processing.
 * Should only construct one TagParsingHelper per file type, instead of one per file.
 */
object TagParsingUtility {
	private val mHelperMap = mutableMapOf<KClass<out Any>, TagParsingHelper<Any>>()

	fun <T> getTagParsingHelper(parser: TextFileParser<T>): TagParsingHelper<T> {
		@Suppress("UNCHECKED_CAST")
		return mHelperMap.getOrPut(parser::class) {
			TagParsingHelper(parser) as TagParsingHelper<Any>
		} as TagParsingHelper<T>
	}

	fun parseIntegerValue(value: String, min: Int, max: Int): Int {
		try {
			return value.toInt().also {
				if (it < min)
					throw MalformedTagException(R.string.intValueTooLow, min, it)
				else if (it > max)
					throw MalformedTagException(R.string.intValueTooHigh, max, it)
			}
		} catch (nfe: NumberFormatException) {
			throw MalformedTagException(R.string.intValueUnreadable, value)
		}
	}

	fun parseDurationValue(value: String, min: Long, max: Long, trackLengthAllowed: Boolean): Long {
		try {
			return Utils.milliToNano(Utils.parseDuration(value, trackLengthAllowed).also {
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
		} catch (nfe: NumberFormatException) {
			throw MalformedTagException(
				BeatPrompter.appResources.getString(
					R.string.durationValueUnreadable,
					value
				)
			)
		}
	}

	fun parseDoubleValue(value: String, min: Int, max: Int): Double {
		try {
			return value.toDouble().also {
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
		} catch (nfe: NumberFormatException) {
			throw MalformedTagException(
				BeatPrompter.appResources.getString(
					R.string.doubleValueUnreadable,
					value
				)
			)
		}
	}

	fun parseColourValue(value: String): Int {
		return try {
			Color.parseColor(value)
		} catch (iae: IllegalArgumentException) {
			try {
				Color.parseColor("#$value")
			} catch (iae2: IllegalArgumentException) {
				throw MalformedTagException(
					BeatPrompter.appResources.getString(
						R.string.colorValueUnreadable,
						value
					)
				)
			}
		}
	}

	fun parseMIDIValue(valueStr: String, argIndex: Int, argCount: Int): Value {
		val trimmedValue = valueStr.trim()
		if (trimmedValue.isEmpty())
			return NoValue()
		try {
			when {
				trimmedValue == "*" -> return WildcardValue()
				trimmedValue.startsWith("?") -> {
					val withoutQuestion = trimmedValue.substring(1)
					try {
						// Arguments are one-based in the alias files.
						return ArgumentValue(Integer.parseInt(withoutQuestion) - 1)
					} catch (nfe: NumberFormatException) {
						throw MalformedTagException(BeatPrompter.appResources.getString(R.string.not_a_valid_argument_index))
					}
				}

				trimmedValue.startsWith("#") -> {
					if (argIndex < argCount - 1)
						throw MalformedTagException(BeatPrompter.appResources.getString(R.string.channel_must_be_last_parameter))
					// Channel is 1-based in text, but 0-based in code.
					return ChannelValue((Utils.parseByte(trimmedValue.substring(1)) - 1).toByte())
				}

				trimmedValue.contains("_") -> {
					if (trimmedValue.indexOf("_") != trimmedValue.lastIndexOf("_"))
						throw MalformedTagException(BeatPrompter.appResources.getString(R.string.multiple_underscores_in_midi_value))
					val zeroedValue = trimmedValue.replace('_', '0')
					if (zeroedValue.looksLikeHex()) {
						return ChanneledCommandValue(Utils.parseHexByte(zeroedValue))
					} else
						throw MalformedTagException(BeatPrompter.appResources.getString(R.string.underscore_in_decimal_value))
				}

				trimmedValue.looksLikeHex() -> return CommandValue(Utils.parseHexByte(trimmedValue))
				trimmedValue.looksLikeDecimal() -> return CommandValue(Utils.parseByte(trimmedValue))
				else -> throw MalformedTagException(BeatPrompter.appResources.getString(R.string.not_a_valid_byte_value))
			}
		} catch (valueEx: ValueException) {
			throw MalformedTagException(valueEx.message!!)
		} catch (nfe: NumberFormatException) {
			throw MalformedTagException(BeatPrompter.appResources.getString(R.string.not_a_valid_byte_value))
		}
	}
}