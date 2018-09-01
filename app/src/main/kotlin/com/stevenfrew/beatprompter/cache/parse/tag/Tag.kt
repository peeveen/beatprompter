package com.stevenfrew.beatprompter.cache.parse.tag

import android.graphics.Color
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Utils

abstract class Tag protected constructor(val mName: String, internal val mLineNumber: Int, val mPosition: Int) {
    companion object {
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
        fun parseDoubleValue(value:String,min: Double, max: Double): Double {
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
    }
}
