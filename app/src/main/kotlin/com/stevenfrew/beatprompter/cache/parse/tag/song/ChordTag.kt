package com.stevenfrew.beatprompter.cache.parse.tag.song

import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import java.util.regex.Pattern

class ChordTag constructor(chordText:String,lineNumber:Int,position:Int): Tag(chordText,lineNumber,position) {
    fun isValidChord():Boolean {
        return chordPattern.matcher(mName.trim()).matches()
    }

    companion object {
        private const val chordRegex = (
                "^([\\s ]*[\\(\\/]{0,2})" //spaces, opening parenthesis,
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

        private val chordPattern = Pattern.compile(chordRegex)
    }
}