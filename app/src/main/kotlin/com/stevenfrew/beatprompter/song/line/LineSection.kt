package com.stevenfrew.beatprompter.song.line

import android.graphics.Paint
import android.graphics.Typeface
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.EndOfHighlightTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.StartOfHighlightTag
import com.stevenfrew.beatprompter.graphics.ColorRect
import com.stevenfrew.beatprompter.graphics.ScreenString
import kotlin.math.max
import kotlin.math.min

class LineSection(
	val lineText: String,
	val chordText: String,
	val isTrueChord: Boolean,
	private val sectionPosition: Int,
	private val tags: Collection<Tag>
) {
	private val trimmedChord = chordText.trim()

	var chordWidth = 0
	var chordHeight = 0
	var chordDescenderOffset = 0
	var chordTrimWidth = 0

	var lineWidth = 0
	var lineHeight = 0
	var lineDescenderOffset = 0

	var chordDrawLine = -1
	var highlightingRectangles =
		mutableListOf<ColorRect>() // Start/stop/start/stop x-coordinates of highlighted sections.

	val width: Int
		get() = max(lineWidth, chordWidth)
	val height: Int
		get() = lineHeight + chordHeight

	fun setTextFontSizeAndMeasure(paint: Paint, fontSize: Int, face: Typeface): Int {
		ScreenString.measure(lineText, paint, fontSize.toFloat(), face)
		lineWidth = ScreenString.mMeasuredWidth
		lineHeight = if (lineText.isBlank())
			0
		else
			ScreenString.mMeasuredHeight
		lineDescenderOffset = ScreenString.mMeasuredDescenderOffset
		return lineWidth
	}

	fun setChordFontSizeAndMeasure(paint: Paint, fontSize: Int, face: Typeface): Int {
		ScreenString.measure(chordText, paint, fontSize.toFloat(), face)
		chordWidth = ScreenString.mMeasuredWidth
		chordHeight = if (trimmedChord.isEmpty())
			0
		else
			ScreenString.mMeasuredHeight
		chordDescenderOffset = ScreenString.mMeasuredDescenderOffset
		chordTrimWidth = if (trimmedChord.length < chordText.length) {
			ScreenString.measure(trimmedChord, paint, fontSize.toFloat(), face)
			ScreenString.mMeasuredWidth
		} else
			chordWidth

		return chordWidth
	}

	fun calculateHighlightedSections(
		paint: Paint,
		textSize: Float,
		face: Typeface,
		currentHighlightColour: Int?
	): Int? {
		var lookingForEnd = currentHighlightColour != null
		var highlightColour = if (lookingForEnd) currentHighlightColour else null
		var startX = 0
		var startPosition = 0
		val highlightTags = tags.filter { it is StartOfHighlightTag || it is EndOfHighlightTag }
		highlightTags.forEach {
			val length = min(it.position - sectionPosition, lineText.length)
			if (it is StartOfHighlightTag && !lookingForEnd) {
				val strHighlightText = lineText.substring(0, length)
				startX = ScreenString.getStringWidth(paint, strHighlightText, face, textSize)
				startPosition = it.position - sectionPosition
				highlightColour = it.color
				lookingForEnd = true
			} else if (it is EndOfHighlightTag && lookingForEnd) {
				val strHighlightText = lineText.substring(startPosition, length)
				val sectionWidth = ScreenString.getStringWidth(paint, strHighlightText, face, textSize)
				highlightingRectangles.add(
					ColorRect(
						startX,
						chordHeight,
						startX + sectionWidth,
						chordHeight + lineHeight,
						highlightColour!!
					)
				)
				highlightColour = null
				lookingForEnd = false
			}
		}
		if (lookingForEnd)
			highlightingRectangles.add(
				ColorRect(
					startX,
					chordHeight,
					max(lineWidth, chordWidth),
					chordHeight + lineHeight,
					highlightColour!!
				)
			)
		return highlightColour
	}

	fun hasChord(): Boolean = chordText.isNotBlank()

	// Even a space can be valid as a section.
	fun hasText(): Boolean = lineText.isNotEmpty()
}
