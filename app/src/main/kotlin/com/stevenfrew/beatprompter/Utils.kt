package com.stevenfrew.beatprompter

import android.graphics.Color
import java.io.*

object Utils {
    internal var mSineLookup = DoubleArray(91)

    // Set by onCreate() in SongList.java
    internal var MAXIMUM_FONT_SIZE: Int = 0
    internal var MINIMUM_FONT_SIZE: Int = 0
    var FONT_SCALING: Float = 0.toFloat()

    // Special token.
    const val TRACK_AUDIO_LENGTH_VALUE = -93781472L
    private val splitters = arrayOf(" ", "-")

    init {
        for (f in 0..90) {
            val radians = Math.toRadians(f.toDouble())
            mSineLookup[f] = Math.sin(radians)
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
        return words.count{!splitters.contains(it)}
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

    fun splitIntoLetters(str: String): List<String> {
        return str.toCharArray().map{it.toString()}
    }

    fun parseDuration(str: String, trackLengthAllowed: Boolean): Long {
        if (str.equals("track", ignoreCase = true) && trackLengthAllowed)
            return Utils.TRACK_AUDIO_LENGTH_VALUE
        try {
            val totalsecs = str.toDouble()
            return Math.floor(totalsecs * 1000.0).toLong()
        } catch (nfe: NumberFormatException) {
            // Might be mm:ss
            val bits=str.split(":")
            if (bits.size==2)
            {
                val mins = bits[0].toInt()
                val secs = bits[1].toInt()
                return (secs + mins * 60) * 1000L
            }
            throw nfe
        }

    }

    @Throws(IOException::class)
    fun streamToStream(instr: InputStream, outstr: OutputStream) {
        val buffer = ByteArray(2048)
        var bytesRead=0
        while (bytesRead != -1) {
            bytesRead = instr.read(buffer, 0, buffer.size)
            if(bytesRead!=-1)
                outstr.write(buffer, 0, bytesRead)
        }
    }

    fun makeSafeFilename(str: String): String {
        return str.replace(Regex("[|\\?*<\":>+\\[\\]/']"),"_")
    }

    @Throws(IOException::class)
    fun appendToTextFile(file: File, str: String) {
        FileWriter(file.absolutePath, true).use { fw -> BufferedWriter(fw).use { bw -> PrintWriter(bw).use { out -> out.println(str) } } }
    }

    private fun stripHexSignifiers(strIn: String): String {
        val str = strIn.toLowerCase()
        if (str.startsWith("0x"))
            return str.substringAfter("0x")
        else if (str.endsWith("h"))
            return str.substring(0, str.length - 1)
        return str
    }

    fun parseHexByte(str: String): Byte {
        return parseByte(stripHexSignifiers(str), 16)
    }

    fun parseByte(str: String): Byte {
        return parseByte(str, 10)
    }

    private fun parseByte(str: String, radix: Int): Byte {
        val byteVal = str.toInt(radix)
        return (byteVal and 0x000000FF).toByte()
    }

    fun looksLikeHex(strIn: String?): Boolean {
        if(strIn==null)
            return false
        val strippedString= stripHexSignifiers(strIn.toLowerCase())
        // Hex values for this app are two-chars long, max.
        return strippedString.matches(Regex("[0-9a-f]{1,2}"))
    }

    /**
     * Replaces weird apostrophe with usual apostrophe ... prevents failed matches based on apostrophe difference.
     * Also remove any stupid BOF character
     */
    fun normalizeString(strIn: String): String {
        return strIn.replace('’', '\'').replace("\uFEFF", "").toLowerCase()
    }
}
