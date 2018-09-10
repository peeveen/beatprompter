package com.stevenfrew.beatprompter

import android.graphics.Paint
import android.graphics.Typeface
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.EndOfHighlightTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.StartOfHighlightTag
import com.stevenfrew.beatprompter.graphics.ColorRect
import com.stevenfrew.beatprompter.graphics.ScreenString

class LineSection constructor(val mLineText: String, val mChordText: String, val mTrueChord:Boolean, private val mSectionPosition: Int, private val mTags: Collection<Tag>) {
    private val mTrimmedChord=mChordText.trim()
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

    fun setTextFontSizeAndMeasure(paint: Paint, fontSize: Int, face: Typeface): Int {
        mLineSS = ScreenString.create(mLineText, paint, fontSize.toFloat(), face)
        mTextHeight = if (mLineText.isBlank())
            0
        else
            mLineSS!!.mHeight
        mTextWidth = mLineSS!!.mWidth
        return mTextWidth
    }

    fun setChordFontSizeAndMeasure(paint: Paint, fontSize: Int, face: Typeface): Int {
        mChordSS = ScreenString.create(mChordText, paint, fontSize.toFloat(), face)
        val trimChordSS: ScreenString = if (mTrimmedChord.length < mChordText.length)
            ScreenString.create(mTrimmedChord, paint, fontSize.toFloat(), face)
        else
            mChordSS!!
        mChordHeight = if (mTrimmedChord.isEmpty())
            0
        else
            mChordSS!!.mHeight
        mChordTrimWidth = trimChordSS.mWidth
        mChordWidth = mChordSS!!.mWidth
        return mChordWidth
    }

    fun calculateHighlightedSections(paint: Paint, textSize: Float, face: Typeface, currentHighlightColour: Int?): Int? {
        var lookingForEnd = currentHighlightColour != null
        var highlightColour = if (lookingForEnd) currentHighlightColour else null
        var startX = 0
        var startPosition = 0
        val highlightTags=mTags.filter{it is StartOfHighlightTag || it is EndOfHighlightTag}
        highlightTags.forEach{
            val length=Math.min(it.mPosition - mSectionPosition,mLineText.length)
            if (it is StartOfHighlightTag && !lookingForEnd) {
                val strHighlightText = mLineText.substring(0, length)
                startX = ScreenString.getStringWidth(paint, strHighlightText, face, textSize)
                startPosition = it.mPosition - mSectionPosition
                highlightColour=it.mColor
                lookingForEnd = true
            } else if (it is EndOfHighlightTag && lookingForEnd) {
                val strHighlightText = mLineText.substring(startPosition, length)
                val sectionWidth = ScreenString.getStringWidth(paint, strHighlightText, face, textSize)
                mHighlightingRectangles.add(ColorRect(startX, mChordHeight, startX + sectionWidth, mChordHeight + mTextHeight, highlightColour!!))
                highlightColour = null
                lookingForEnd = false
            }
        }
        if (lookingForEnd)
            mHighlightingRectangles.add(ColorRect(startX, mChordHeight, Math.max(mTextWidth, mChordWidth), mChordHeight + mTextHeight, highlightColour!!))
        return highlightColour
    }

    fun hasChord(): Boolean {
        return mChordText.isNotBlank()
    }

    fun hasText(): Boolean {
        // Even a space can be valid as a section.
        return mLineText.isNotEmpty()
    }
}
