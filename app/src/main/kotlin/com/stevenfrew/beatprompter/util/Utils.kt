package com.stevenfrew.beatprompter.util

import android.graphics.Color
import java.io.*
import kotlin.math.floor
import kotlin.math.sin

object Utils {
	internal var mSineLookup = DoubleArray(91)

	// Size of a "long", in bytes.
	internal const val LONG_BUFFER_SIZE = 8

	// Set by onCreate() in SongListActivity.java
	internal var MAXIMUM_FONT_SIZE: Int = 0
	internal var MINIMUM_FONT_SIZE: Int = 0
	var FONT_SCALING: Float = 0.toFloat()

	// Special token.
	const val TRACK_AUDIO_LENGTH_VALUE = -93781472L
	private val splitters = arrayOf(" ", "-")

	init {
		repeat(91) {
			val radians = Math.toRadians(it.toDouble())
			mSineLookup[it] = sin(radians)
		}
	}

	fun nanosecondsPerBeat(bpm: Double): Long {
		return (60000000000.0 / bpm).toLong()
	}

	fun nanoToMilli(nano: Long): Int {
		return (nano / 1000000).toInt()
	}

	fun milliToNano(milli: Int): Long {
		return milli.toLong() * 1000000
	}

	fun milliToNano(milli: Long): Long {
		return milli * 1000000
	}

	fun bpmToMIDIClockNanoseconds(bpm: Double): Double {
		return 60000000000.0 / (bpm * 24.0)
	}

	fun makeHighlightColour(vColour: Int): Int {
		var colour = vColour
		colour = colour and 0x00ffffff
		colour = colour or 0x44000000
		return colour
	}

	fun makeHighlightColour(vColour: Int, opacity: Byte): Int {
		var colour = vColour
		colour = colour and 0x00ffffff
		colour = colour or (opacity.toInt() shl 24)
		return colour
	}

	fun makeContrastingColour(colour: Int): Int {
		val r = colour shr 16 and 0x000000FF
		val g = colour shr 8 and 0x000000FF
		val b = colour and 0x000000FF
		return if (r * 0.299 + g * 0.587 + b * 0.114 > 186) Color.BLACK else Color.WHITE
	}

	fun countWords(words: List<String>): Int {
		return words.count { !splitters.contains(it) }
	}

	fun stitchBits(bits: List<String>, nonWhitespaceBitsToJoin: Int): String {
		val result = StringBuilder()
		var nonWhitespaceBitsJoined = 0
		for (bit in bits) {
			val whitespace = bit.isBlank()
			if (!whitespace && nonWhitespaceBitsJoined == nonWhitespaceBitsToJoin)
				break
			result.append(bit)
			if (!whitespace)
				++nonWhitespaceBitsJoined
		}
		return result.toString()
	}

	fun splitText(strIn: String): List<String> {
		return strIn.split(Regex("(?<=[ -])|(?=[ -])"))
	}

	/**
	 * Returns milliseconds value
	 */
	fun parseDuration(str: String, trackLengthAllowed: Boolean): Long {
		if (str.equals("track", ignoreCase = true) && trackLengthAllowed)
			return TRACK_AUDIO_LENGTH_VALUE
		try {
			val totalSecs = str.toDouble()
			return floor(totalSecs * 1000.0).toLong()
		} catch (nfe: NumberFormatException) {
			// Might be mm:ss
			val bits = str.splitAndTrim(":")
			if (bits.size == 2) {
				val minutes = bits[0].toInt()
				val secs = bits[1].toInt()
				return (secs + (minutes * 60)) * 1000L
			}
			throw nfe
		}

	}

	fun streamToStream(inStream: InputStream, outStream: OutputStream) {
		val buffer = ByteArray(2048)
		var bytesRead = 0
		while (bytesRead != -1) {
			bytesRead = inStream.read(buffer, 0, buffer.size)
			if (bytesRead != -1)
				outStream.write(buffer, 0, bytesRead)
		}
	}

	fun makeSafeFilename(str: String): String {
		@Suppress("RegExpRedundantEscape")
		return str.replace(Regex("[|\\?*<\":>+\\[\\]/']"), "_")
	}

	fun appendToTextFile(file: File, str: String) {
		FileWriter(
			file.absolutePath,
			true
		).use { fw -> BufferedWriter(fw).use { bw -> PrintWriter(bw).use { out -> out.println(str) } } }
	}

	fun parseHexByte(str: String): Byte {
		return parseByte(str.stripHexSignifiers(), 16)
	}

	fun parseByte(str: String): Byte {
		return parseByte(str, 10)
	}

	private fun parseByte(str: String, radix: Int): Byte {
		val byteVal = str.toInt(radix)
		return (byteVal and 0x000000FF).toByte()
	}

	fun safeThreadWait(amount: Long) {
		try {
			Thread.sleep(amount)
		} catch (ie: InterruptedException) {
		}
	}
}