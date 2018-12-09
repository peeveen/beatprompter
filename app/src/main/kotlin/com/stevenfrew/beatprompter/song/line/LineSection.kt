package com.stevenfrew.beatprompter.song.line

import android.graphics.Paint
import android.graphics.Typeface
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.EndOfHighlightTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.StartOfHighlightTag
import com.stevenfrew.beatprompter.graphics.ColorRect
import com.stevenfrew.beatprompter.graphics.ScreenString

class LineSection constructor(val mLineText: String,
                              val mChordText: String,
                              val mTrueChord: Boolean,
                              private val mSectionPosition: Int,
                              private val mTags: Collection<Tag>) {
    private val mTrimmedChord = mChordText.trim()
    var mChordWidth = 0
    var mChordHeight = 0
    var mChordDescenderOffset = 0
    var mChordTrimWidth = 0

    var mLineWidth = 0
    var mLineHeight = 0
    var mLineDescenderOffset = 0

    var mChordDrawLine = -1
    var mHighlightingRectangles = mutableListOf<ColorRect>() // Start/stop/start/stop x-coordinates of highlighted sections.

    val width: Int
        get() = Math.max(mLineWidth, mChordWidth)
    val height: Int
        get() = mLineHeight + mChordHeight

    fun setTextFontSizeAndMeasure(paint: Paint, fontSize: Int, face: Typeface): Int {
        ScreenString.measure(mLineText, paint, fontSize.toFloat(), face)
        mLineWidth = ScreenString.mMeasuredWidth
        mLineHeight = if (mLineText.isBlank())
            0
        else
            ScreenString.mMeasuredHeight
        mLineDescenderOffset = ScreenString.mMeasuredDescenderOffset
        return mLineWidth
    }

    fun setChordFontSizeAndMeasure(paint: Paint, fontSize: Int, face: Typeface): Int {
        ScreenString.measure(mChordText, paint, fontSize.toFloat(), face)
        mChordWidth = ScreenString.mMeasuredWidth
        mChordHeight = if (mTrimmedChord.isEmpty())
            0
        else
            ScreenString.mMeasuredHeight
        mChordDescenderOffset = ScreenString.mMeasuredDescenderOffset
        mChordTrimWidth = if (mTrimmedChord.length < mChordText.length) {
            ScreenString.measure(mTrimmedChord, paint, fontSize.toFloat(), face)
            ScreenString.mMeasuredWidth
        } else
            mChordWidth

        return mChordWidth
    }

    fun calculateHighlightedSections(paint: Paint, textSize: Float, face: Typeface, currentHighlightColour: Int?): Int? {
        var lookingForEnd = currentHighlightColour != null
        var highlightColour = if (lookingForEnd) currentHighlightColour else null
        var startX = 0
        var startPosition = 0
        val highlightTags = mTags.filter { it is StartOfHighlightTag || it is EndOfHighlightTag }
        highlightTags.forEach {
            val length = Math.min(it.mPosition - mSectionPosition, mLineText.length)
            if (it is StartOfHighlightTag && !lookingForEnd) {
                val strHighlightText = mLineText.substring(0, length)
                startX = ScreenString.getStringWidth(paint, strHighlightText, face, textSize)
                startPosition = it.mPosition - mSectionPosition
                highlightColour = it.mColor
                lookingForEnd = true
            } else if (it is EndOfHighlightTag && lookingForEnd) {
                val strHighlightText = mLineText.substring(startPosition, length)
                val sectionWidth = ScreenString.getStringWidth(paint, strHighlightText, face, textSize)
                mHighlightingRectangles.add(ColorRect(startX, mChordHeight, startX + sectionWidth, mChordHeight + mLineHeight, highlightColour!!))
                highlightColour = null
                lookingForEnd = false
            }
        }
        if (lookingForEnd)
            mHighlightingRectangles.add(ColorRect(startX, mChordHeight, Math.max(mLineWidth, mChordWidth), mChordHeight + mLineHeight, highlightColour!!))
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
