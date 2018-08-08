package com.stevenfrew.beatprompter.midi

import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Utils
import com.stevenfrew.beatprompter.cache.FileParseError
import java.util.ArrayList
import kotlin.experimental.and

/**
 * A value in a MIDI component definition.
 * It can be a simple byte value, (CommandValue)
 * or a partial byte value with channel specifier, (ChannelCommandValue)
 * or a reference to an argument (ArgumentValue)
 */
abstract class Value {
    @Throws(ResolutionException::class)
    internal abstract fun resolve(arguments: ByteArray, channel: Byte): Byte

    internal abstract fun matches(otherValue: Value?): Boolean

    @Throws(ResolutionException::class)
    fun resolve(): Byte {
        return resolve(ByteArray(0), 0.toByte())
    }

    companion object {
        fun parseValue(strVal: String): Value {
            return parseValue(strVal, 0, 0, 1, ArrayList())
        }

        fun parseChannelValue(channelValStr: String): Value {
            var strVal = channelValStr
            if (!strVal.isEmpty())
                if (!strVal.startsWith("*"))
                    if (!strVal.startsWith("#"))
                        strVal = "#$strVal"
            return parseValue(strVal)
        }

        fun parseValue(valueStr: String, lineNumber: Int, argIndex: Int, argCount: Int, errors: MutableList<FileParseError>): Value {
            var strVal = valueStr
            strVal = strVal.trim { it <= ' ' }
            if (strVal.isEmpty())
                return NoValue()
            if (strVal == "*")
                return WildcardValue()
            else if (strVal.startsWith("?")) {
                strVal = strVal.substring(1)
                try {
                    val referencedArgIndex = Integer.parseInt(strVal)
                    if (referencedArgIndex < 0)
                        errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.not_a_valid_argument_index)))
                    else
                        return ArgumentValue(referencedArgIndex)
                } catch (nfe: NumberFormatException) {
                    errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.not_a_valid_argument_index)))
                }

            } else if (strVal.startsWith("#")) {
                if (argIndex < argCount - 1)
                    errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.channel_must_be_last_parameter)))
                try {
                    val channel = Utils.parseByte(strVal.substring(1))
                    if (channel < 1 || channel > 16)
                        errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.invalid_channel_value)))
                    return ChannelValue(channel)
                } catch (nfe: NumberFormatException) {
                    errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.not_a_valid_byte_value)))
                }

            } else if (strVal.contains("_")) {
                if (strVal.indexOf("_") != strVal.lastIndexOf("_"))
                    errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.multiple_underscores_in_midi_value)))
                strVal = strVal.replace('_', '0')
                try {
                    if (Utils.looksLikeHex(strVal)) {
                        val channelValue = Utils.parseHexByte(strVal)
                        if (channelValue and 0x0F != 0.toByte())
                            errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.merge_with_channel_non_zero_lower_nibble)))
                        else
                            return ChanneledCommandValue(channelValue)
                    } else
                        errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.underscore_in_decimal_value)))
                } catch (nfe: NumberFormatException) {
                    errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.not_a_valid_byte_value)))
                }

            } else if (Utils.looksLikeHex(strVal)) {
                try {
                    return CommandValue(Utils.parseHexByte(strVal))
                } catch (nfe: NumberFormatException) {
                    errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.not_a_valid_byte_value)))
                }

            } else {
                try {
                    return CommandValue(Utils.parseByte(strVal))
                } catch (nfe: NumberFormatException) {
                    errors.add(FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.not_a_valid_byte_value)))
                }
            }
            return NoValue()
        }
    }
}