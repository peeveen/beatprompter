package com.stevenfrew.beatprompter

import android.graphics.Color
import java.io.*
import java.util.regex.Pattern

object Utils {
    internal var mSineLookup = DoubleArray(91)

    // Set by onCreate() in SongList.java
    internal var MAXIMUM_FONT_SIZE: Int = 0
    internal var MINIMUM_FONT_SIZE: Int = 0
    var FONT_SCALING: Float = 0.toFloat()

    // Special token.
    const val TRACK_AUDIO_LENGTH_VALUE = -93781472
    private val splitters = listOf(" ", "-")

    private const val REGEXP = (
            "^([\\s ]*[\\(\\/]{0,2})" //spaces, opening parenthesis, /

                    + "(([ABCDEFG])([b\u266D#\u266F\u266E])?)" //note name + accidental

                    //\u266D = flat, \u266E = natural, \u266F = sharp
                    + "([mM1234567890abdijnsu��o�\u00D8\u00F8\u00B0\u0394\u2206\\-\\+]*)"
                    //handles min(or), Maj/maj(or), dim, sus, Maj7, mb5...
                    // but not #11 (may be ok for Eb7#11,
                    // but F#11 will disturb...)
                    //\u00F8 = slashed o, \u00D8 = slashed O, \u00B0 = degree
                    //(html ø, Ø, °)
                    //delta = Maj7, maths=\u2206, greek=\u0394
                    + "((\\/)(([ABCDEFG])([b\u266D#\u266F\u266E])?))?" // /bass

                    + "(\\)?[ \\s]*)$") //closing parenthesis, spaces

    private val pattern = Pattern.compile(REGEXP)

    private const val ReservedChars = "|\\?*<\":>+[]/'"

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
            val whitespace = bit.trim().isEmpty()
            if (!whitespace && nonWhitespaceBitsJoined == nonWhitespaceBitsToJoin)
                break
            result.append(bit)
            if (!whitespace)
                ++nonWhitespaceBitsJoined
        }
        return result.toString()
    }

    fun splitText(strIn: String): List<String> {
        var str = strIn
        val bits = mutableListOf<String>()
        var bestSplitIndex: Int
        while (true) {
            bestSplitIndex = str.length
            var bestSplitter: String? = null
            for (splitter in splitters) {
                val splitIndex = str.indexOf(splitter)
                if (splitIndex != -1) {
                    bestSplitIndex = Math.min(splitIndex, bestSplitIndex)
                    if (bestSplitIndex == splitIndex)
                        bestSplitter = splitter
                }
            }
            val bit = str.substring(0, bestSplitIndex)
            if (bit.isNotEmpty())
                bits.add(bit)
            if (bestSplitter != null) {
                bits.add(bestSplitter)
                str = str.substring(bestSplitIndex + 1)
            } else
                break
        }
        return bits.toList()
    }

    fun splitIntoLetters(str: String): List<String> {
        return str.toCharArray().map{it.toString()}
    }

    fun parseDuration(str: String, trackLengthAllowed: Boolean): Int {
        if (str.equals("track", ignoreCase = true) && trackLengthAllowed)
            return Utils.TRACK_AUDIO_LENGTH_VALUE
        try {
            val totalsecs = java.lang.Double.parseDouble(str)
            return Math.floor(totalsecs * 1000.0).toInt()
        } catch (nfe: NumberFormatException) {
            // Might be mm:ss
            val colonIndex = str.indexOf(":")
            if (colonIndex != -1 && colonIndex < str.length - 1) {
                val strMins = str.substring(0, colonIndex)
                val strSecs = str.substring(colonIndex + 1)
                val mins = Integer.parseInt(strMins)
                val secs = Integer.parseInt(strSecs)
                return (secs + mins * 60) * 1000
            }
            throw nfe
        }

    }

    fun isChord(textIn: String?): Boolean {
        var text = textIn
        if (text != null)
            text = text.trim()
        return !(text == null || text.isEmpty()) && pattern.matcher(text).matches()
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
        val builder = StringBuilder()
        for (c in str.toCharArray())
            if (ReservedChars.contains("" + c))
                builder.append("_")
            else
                builder.append(c)
        return builder.toString()
    }

    @Throws(IOException::class)
    fun appendToTextFile(file: File, str: String) {
        FileWriter(file.absolutePath, true).use { fw -> BufferedWriter(fw).use { bw -> PrintWriter(bw).use { out -> out.println(str) } } }
    }

    private fun stripHexSignifiers(strIn: String): String {
        var str = strIn
        str = str.toLowerCase()
        if (str.startsWith("0x"))
            str = str.substring(2)
        else if (str.endsWith("h"))
            str = str.substring(0, str.length - 1)
        return str
    }

    fun parseHexByte(str: String): Byte {
        return parseByte(stripHexSignifiers(str), 16)
    }

    fun parseByte(str: String): Byte {
        return parseByte(str, 10)
    }

    private fun parseByte(str: String, radix: Int): Byte {
        val `val` = Integer.parseInt(str, radix)
        return (`val` and 0x000000FF).toByte()
    }

    fun looksLikeHex(strIn: String?): Boolean {
        var str: String? = strIn ?: return false
        str = str!!.toLowerCase()
        var signifierFound = false
        if (str.startsWith("0x")) {
            signifierFound = true
            str = str.substring(2)
        } else if (str.endsWith("h")) {
            signifierFound = true
            str = str.substring(0, str.length - 1)
        }
        // Hex values for this app are two-chars long, max.
        if (str.length > 2)
            return false
        try {

            Integer.parseInt(str)
            // non-hex integer
            return signifierFound
        } catch (ignored: Exception) {
        }

        for (f in 0 until str.length) {
            val c = str[f]
            if (!Character.isDigit(c) && c != 'a' && c != 'b' && c != 'c' && c != 'd' && c != 'e' && c != 'f')
                return false
        }
        return true
    }
}
