package com.stevenfrew.beatprompter.cache.parse.tag

import android.graphics.Color
import com.stevenfrew.beatprompter.*
import com.stevenfrew.beatprompter.cache.parse.TextFileParser
import com.stevenfrew.beatprompter.midi.alias.*
import kotlin.experimental.and
import kotlin.reflect.KClass

/**
 * Singleton map of parser-type to TagParsingHelper. Saves a lot of annotation processing.
 * Should only construct one TagParsingHelper per file type, instead of one per file.
 */
object TagParsingUtility {
    private val mHelperMap=mutableMapOf<KClass<out Any>, TagParsingHelper<Any>>()
    fun <T> getTagParsingHelper(parser: TextFileParser<T>): TagParsingHelper<T>
    {
        return mHelperMap.getOrPut(parser::class) { TagParsingHelper(parser) as TagParsingHelper<Any> } as TagParsingHelper<T>
    }

    @Throws(MalformedTagException::class)
    fun parseIntegerValue(value:String,min: Int, max: Int): Int {
        val intVal: Int
        try {
            intVal = value.toInt()
            if (intVal < min)
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.intValueTooLow, min, intVal))
            else if (intVal > max)
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.intValueTooHigh, max, intVal))
        } catch (nfe: NumberFormatException) {
            throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.intValueUnreadable, value))
        }
        return intVal
    }

    @Throws(MalformedTagException::class)
    fun parseDurationValue(value:String,min: Long, max: Long, trackLengthAllowed: Boolean): Long {
        val durVal: Long
        try {
            durVal = Utils.parseDuration(value, trackLengthAllowed)
            if (durVal < min && durVal != Utils.TRACK_AUDIO_LENGTH_VALUE)
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.intValueTooLow, min, durVal))
            else if (durVal > max)
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.intValueTooHigh, max, durVal))
        } catch (nfe: NumberFormatException) {
            throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.durationValueUnreadable, value))
        }
        return Utils.milliToNano(durVal)
    }

    @Throws(MalformedTagException::class)
    fun parseDoubleValue(value:String,min: Int, max: Int): Double {
        val doubleVal: Double
        try {
            doubleVal = value.toDouble()
            if (doubleVal < min)
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.doubleValueTooLow, min, doubleVal))
            else if (doubleVal > max)
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.doubleValueTooHigh, max, doubleVal))
        } catch (nfe: NumberFormatException) {
            throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.doubleValueUnreadable, value))
        }
        return doubleVal
    }

    @Throws(MalformedTagException::class)
    fun parseColourValue(value:String): Int {
        return try {
            Color.parseColor(value)
        } catch (iae: IllegalArgumentException) {
            try {
                Color.parseColor("#$value")
            } catch (iae2: IllegalArgumentException) {
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.colorValueUnreadable, value))
            }
        }
    }

    @Throws(MalformedTagException::class)
    fun parseMIDIValue(valueStr: String, argIndex: Int, argCount: Int): Value {
        var strVal = valueStr
        strVal = strVal.trim()
        if (strVal.isEmpty())
            return NoValue()
        when {
            strVal == "*" -> return WildcardValue()
            strVal.startsWith("?") -> {
                strVal = strVal.substring(1)
                try {
                    val referencedArgIndex = Integer.parseInt(strVal)
                    // Arguments are one-based in the alias files.
                    if (referencedArgIndex <= 0)
                        throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_argument_index))
                    else
                        return ArgumentValue(referencedArgIndex - 1)
                } catch (nfe: NumberFormatException) {
                    throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_argument_index))
                }

            }
            strVal.startsWith("#") -> {
                if (argIndex < argCount - 1)
                    throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.channel_must_be_last_parameter))
                try {
                    val channel = Utils.parseByte(strVal.substring(1))
                    if (channel < 1 || channel > 16)
                        throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.invalid_channel_value))
                    // Channel is 1-based in text, but 0-based in code.
                    return ChannelValue((channel - 1).toByte())
                } catch (nfe: NumberFormatException) {
                    throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_byte_value))
                }

            }
            strVal.contains("_") -> {
                if (strVal.indexOf("_") != strVal.lastIndexOf("_"))
                    throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.multiple_underscores_in_midi_value))
                strVal = strVal.replace('_', '0')
                try {
                    if (strVal.looksLikeHex()) {
                        val channelValue = Utils.parseHexByte(strVal)
                        if (channelValue and 0x0F != 0.toByte())
                            throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.merge_with_channel_non_zero_lower_nibble))
                        else
                            return ChanneledCommandValue(channelValue)
                    } else
                        throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.underscore_in_decimal_value))
                } catch (nfe: NumberFormatException) {
                    throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_byte_value))
                }

            }
            strVal.looksLikeHex() -> try {
                return CommandValue(Utils.parseHexByte(strVal))
            } catch (nfe: NumberFormatException) {
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_byte_value))
            }
            strVal.looksLikeDecimal() -> try {
                return CommandValue(Utils.parseByte(strVal))
            } catch (nfe: NumberFormatException) {
                throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_byte_value))
            }
            else->throw MalformedTagException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_byte_value))
        }
    }
}