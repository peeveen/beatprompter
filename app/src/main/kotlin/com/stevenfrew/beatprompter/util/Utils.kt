package com.stevenfrew.beatprompter.util

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import com.stevenfrew.beatprompter.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import kotlin.math.floor
import kotlin.math.sin

object Utils {
	internal var mSineLookup = DoubleArray(91)

	// Size of a "long", in bytes.
	internal const val LONG_BUFFER_SIZE = 8

	// Special token.
	const val TRACK_AUDIO_LENGTH_VALUE = -93781472L
	private val splitters = arrayOf(" ", "-")

	init {
		repeat(91) {
			val radians = Math.toRadians(it.toDouble())
			mSineLookup[it] = sin(radians)
		}
	}

	fun showExceptionDialog(t: Throwable, context: Context) =
		showMessageDialog(t.message ?: "<unknown>", R.string.errorTitle, context)

	fun showMessageDialog(text: String, title: Int, context: Context) {
		AlertDialog.Builder(context).apply { setMessage(text) }.create().apply {
			setCanceledOnTouchOutside(true)
			setTitle(title)
			setButton(
				android.app.AlertDialog.BUTTON_NEUTRAL, "OK"
			) { dialog, _ -> dialog.dismiss() }
			show()
		}
	}

	fun nanosecondsPerBeat(bpm: Double): Long = (60000000000.0 / bpm).toLong()
	fun nanoToMilli(nano: Long): Int = (nano / 1000000).toInt()
	fun milliToNano(milli: Int): Long = milli.toLong() * 1000000
	fun milliToNano(milli: Long): Long = milli * 1000000
	fun bpmToMIDIClockNanoseconds(bpm: Double): Double = 60000000000.0 / (bpm * 24.0)

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

	fun countWords(words: List<String>): Int = words.count { !splitters.contains(it) }

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

	fun splitText(strIn: String): List<String> = strIn.split(Regex("(?<=[ -])|(?=[ -])"))

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

	@Suppress("RegExpRedundantEscape")
	fun makeSafeFilename(str: String): String = str.replace(Regex("[|\\?*<\":>+\\[\\]/']"), "_")

	fun appendToTextFile(file: File, str: String) =
		FileWriter(
			file.absolutePath,
			true
		).use { fw -> BufferedWriter(fw).use { bw -> PrintWriter(bw).use { out -> out.println(str) } } }

	fun parseHexByte(str: String): Byte = parseByte(str.stripHexSignifiers(), 16)
	fun parseByte(str: String): Byte = parseByte(str, 10)
	private fun parseByte(str: String, radix: Int): Byte = (str.toInt(radix) and 0x000000FF).toByte()

	fun safeThreadWait(amount: Long) =
		try {
			Thread.sleep(amount)
		} catch (_: InterruptedException) {
			// Ignore
		}

	fun <T> reportProgress(listener: ProgressReportingListener<T>, message: T) =
		runBlocking {
			launch {
				listener.onProgressMessageReceived(message)
			}
		}

	private fun getReachableCitiesAndDistances(
		currentCity: Int,
		currentResults: Map<Int, List<Pair<Int, Int>>>,
		edges: List<IntArray>,
		distanceThreshold: Int
	): List<Pair<Int, Int>> {
		val (edgesToOrFromThisCity, otherEdges) =
			edges.partition { it[0] == currentCity || it[1] == currentCity }
		// Cities we can reach directly.
		val directTraversals =
			edgesToOrFromThisCity.filter { it[2] <= distanceThreshold }
				.map { Pair(if (it[1] == currentCity) it[0] else it[1], it[2]) }
		println("Direct traversals from $currentCity = ${directTraversals.count()}")
		// Cities we ALREADY KNOW we can reach based on previous results.
		val traversalsFromResults =
			currentResults.flatMap { currentResult ->
				currentResult.value.filter { it.first == currentCity }.map {
					Pair(
						currentResult.key,
						it.second
					)
				}
			}
		println("Traversals from results from $currentCity = ${traversalsFromResults.count()}")
		// Cities we ALREADY KNOW we can reach indirectly based on previous results.
		val indirectTraversalsFromResults = directTraversals.flatMap { directTraversal ->
			currentResults[directTraversal.first]?.filter {
				it.first != currentCity && it.second + directTraversal.second <= distanceThreshold
			}?.map { Pair(it.first, it.second + directTraversal.second) }
				?: listOf()
		}
		println("Indirect traversals from results from $currentCity = ${indirectTraversalsFromResults.count()}")
		// Cities we can reach indirectly by calculation.
		val indirectTraversals = directTraversals.flatMap {
			if (distanceThreshold - it.second > 0)
				getReachableCitiesAndDistances(
					it.first,
					currentResults,
					otherEdges,
					distanceThreshold - it.second
				)
			else listOf()
		}
		println("Indirect traversals from $currentCity = ${indirectTraversals.count()}")
		return listOf(
			directTraversals,
			traversalsFromResults,
			indirectTraversalsFromResults,
			indirectTraversals
		).flatten()
	}

	fun findTheCity(n: Int, edges: Array<IntArray>, distanceThreshold: Int): Int =
		// Key is city number, value is list of tuples (reachable city number, and distance to it)
		mutableMapOf<Int, List<Pair<Int, Int>>>().let { results ->
			edges.toList().let { edgesList ->
				((n - 1).downTo(0))
					.fold(results) { acc, city ->
						acc.also {
							it[city] =
								getReachableCitiesAndDistances(
									city,
									acc,
									// Get rid of edges that will already have been traversed in earlier results.
									edgesList.filter { edge -> edge[0] <= city && edge[1] <= city },
									distanceThreshold
								)
							println("Reachable from $city = ${it[city]?.distinctBy { x -> x.first }?.count()}")
						}
					}
			}
		}.minBy { kvp -> kvp.value.distinctBy { it.first }.count() }.key
}