package com.stevenfrew.beatprompter

import android.graphics.Paint
import android.graphics.Typeface
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.EndOfHighlightTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.StartOfHighlightTag

class LineSection(var mLineText: String?, var mChordText: String?, val mTrueChord:Boolean, private val mSectionPosition: Int, private val mTags: Collection<Tag>) {
    var mNextSection: LineSection? = null
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

    fun calculateHighlightedSections(paint: Paint, textSize: Float, face: Typeface, currentHighlightColour: Int): Int {
        var lookingForEnd = currentHighlightColour != 0
        var highlightColour = if (lookingForEnd) currentHighlightColour else 0
        var startX = 0
        var startPosition = 0
        val highlightTags=mTags.filter{it is StartOfHighlightTag || it is EndOfHighlightTag}
        highlightTags.forEach{
            if (it is StartOfHighlightTag && !lookingForEnd) {
                val strHighlightText = mLineText!!.substring(0, it.mPosition - mSectionPosition)
                startX = ScreenString.getStringWidth(paint, strHighlightText, face, textSize)
                startPosition = it.mPosition - mSectionPosition
                highlightColour=it.mColor
                lookingForEnd = true
            } else if (it is EndOfHighlightTag && lookingForEnd) {
                val strHighlightText = mLineText!!.substring(startPosition, it.mPosition - mSectionPosition)
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
