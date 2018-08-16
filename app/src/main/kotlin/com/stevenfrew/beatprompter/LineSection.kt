package com.stevenfrew.beatprompter

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.cache.parse.tag.Tag

internal class LineSection(var mLineText: String?, var mChordText: String?, private val mSectionPosition: Int, private val mTags: Collection<Tag>) {
    var mNextSection: LineSection? = null
    var mPrevSection: LineSection? = null
    val mIsChord: Boolean= Utils.isChord(mChordText)
    var mTextWidth = 0
    var mChordWidth = 0
    var mChordTrimWidth = 0
    var mTextHeight = 0
    var mChordHeight = 0
    var mChordDrawLine = -1
    var mLineSS: ScreenString?=null
    var mChordSS: ScreenString?=null
    var mHighlightingRectangles = mutableListOf<ColorRect>() // Start/stop/start/stop x-coordinates of highlighted sections.

    val width: Int
        get() = Math.max(mTextWidth, mChordWidth)
    val height: Int
        get() = mTextHeight + mChordHeight

    fun setTextFontSizeAndMeasure(paint: Paint, fontSize: Int, face: Typeface, color: Int): Int {
        mLineSS = ScreenString.create(mLineText!!, paint, fontSize.toFloat(), face, color)
        mTextHeight = if (mLineText!!.trim().isEmpty())
            0
        else
            mLineSS!!.mHeight
        mTextWidth = mLineSS!!.mWidth
        return mTextWidth
    }

    fun setChordFontSizeAndMeasure(paint: Paint, fontSize: Int, face: Typeface, color: Int): Int {
        mChordSS = ScreenString.create(mChordText!!, paint, fontSize.toFloat(), face, color)
        val trimChord = mChordText!!.trim()
        val trimChordSS: ScreenString
        trimChordSS = if (trimChord.length < mChordText!!.length)
            ScreenString.create(trimChord, paint, fontSize.toFloat(), face, color)
        else
            mChordSS!!
        mChordHeight = if (mChordText!!.trim().isEmpty())
            0
        else
            mChordSS!!.mHeight
        mChordTrimWidth = trimChordSS.mWidth
        mChordWidth = mChordSS!!.mWidth
        return mChordWidth
    }

    fun calculateHighlightedSections(paint: Paint, textSize: Float, face: Typeface, currentHighlightColour: Int, defaultHighlightColour: Int, errors: MutableList<FileParseError>): Int {
        var lookingForEnd = currentHighlightColour != 0
        var highlightColour = if (lookingForEnd) currentHighlightColour else 0
        var startX = 0
        var startPosition = 0
        for (tag in mTags) {
            if (tag.mName == "soh" && !lookingForEnd) {
                val strHighlightText = mLineText!!.substring(0, tag.mPosition - mSectionPosition)
                startX = ScreenString.getStringWidth(paint, strHighlightText, face, textSize)
                startPosition = tag.mPosition - mSectionPosition
                if (tag.mValue.isNotEmpty()) {
                    highlightColour = try {
                        Color.parseColor(tag.mValue)
                    } catch (e: IllegalArgumentException) {
                        // We're past the titlescreen at this point, so don't bother.
                        errors.add(FileParseError(tag, "Could not interpret \"" + tag.mValue + "\" as a valid colour. Using default."))
                        defaultHighlightColour
                    }

                    highlightColour = Utils.makeHighlightColour(highlightColour)
                } else
                    highlightColour = defaultHighlightColour
                lookingForEnd = true
            } else if (tag.mName == "eoh" && lookingForEnd) {
                val strHighlightText = mLineText!!.substring(startPosition, tag.mPosition - mSectionPosition)
                val sectionWidth = ScreenString.getStringWidth(paint, strHighlightText, face, textSize)
                mHighlightingRectangles.add(ColorRect(startX, mChordHeight, startX + sectionWidth, mChordHeight + mTextHeight, highlightColour))
                highlightColour = 0
                lookingForEnd = false
            }
        }
        if (lookingForEnd)
            mHighlightingRectangles.add(ColorRect(startX, mChordHeight, Math.max(mTextWidth, mChordWidth), mChordHeight + mTextHeight, highlightColour))
        return highlightColour
    }

    fun hasChord(): Boolean {
        return mChordText != null && mChordText!!.trim().isNotEmpty()
    }

    fun hasText(): Boolean {
        return mLineText != null && mLineText!!.trim().isNotEmpty()
    }

}
