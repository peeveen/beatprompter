package com.stevenfrew.beatprompter.song.line

import android.graphics.Paint
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.EndOfHighlightTag
import com.stevenfrew.beatprompter.cache.parse.tag.song.StartOfHighlightTag
import com.stevenfrew.beatprompter.graphics.ColorRect
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

	fun setTextFontSizeAndMeasure(paint: Paint, fontSize: Int): Int {
		val measurement =
			BeatPrompter.platformUtils.fontManager.measure(lineText, paint, fontSize.toFloat())
		lineWidth = measurement.width
		lineHeight = if (lineText.isBlank())
			0
		else
			measurement.height
		lineDescenderOffset = measurement.descenderOffset
		return lineWidth
	}

	fun setChordFontSizeAndMeasure(paint: Paint, fontSize: Int): Int {
		val measurement =
			BeatPrompter.platformUtils.fontManager.measure(chordText, paint, fontSize.toFloat())
		chordWidth = measurement.width
		chordHeight = if (trimmedChord.isEmpty())
			0
		else
			measurement.height
		chordDescenderOffset = measurement.descenderOffset
		chordTrimWidth = if (trimmedChord.length < chordText.length) {
			val newMeasurement =
				BeatPrompter.platformUtils.fontManager.measure(trimmedChord, paint, fontSize.toFloat())
			newMeasurement.width
		} else
			chordWidth

		return chordWidth
	}

	fun calculateHighlightedSections(
		paint: Paint,
		textSize: Float,
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
				val stringWidth =
					BeatPrompter.platformUtils.fontManager.getStringWidth(paint, strHighlightText, textSize)
				startX = stringWidth.first
				startPosition = it.position - sectionPosition
				highlightColour = it.color
				lookingForEnd = true
			} else if (it is EndOfHighlightTag && lookingForEnd) {
				val strHighlightText = lineText.substring(startPosition, length)
				val sectionWidth =
					BeatPrompter.platformUtils.fontManager.getStringWidth(paint, strHighlightText, textSize)
				highlightingRectangles.add(
					ColorRect(
						startX,
						chordHeight,
						startX + sectionWidth.first,
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
