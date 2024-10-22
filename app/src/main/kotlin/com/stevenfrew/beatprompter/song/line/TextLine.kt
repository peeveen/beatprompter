package com.stevenfrew.beatprompter.song.line

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ChordTag
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.song.chord.Chord
import com.stevenfrew.beatprompter.song.chord.ChordMap
import com.stevenfrew.beatprompter.song.load.SongLoadCancelEvent
import com.stevenfrew.beatprompter.song.load.SongLoadCancelledException
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.characters
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class TextLine internal constructor(
	private val text: String,
	private val tags: List<Tag>,
	lineTime: Long,
	lineDuration: Long,
	scrollMode: ScrollingMode,
	displaySettings: DisplaySettings,
	startingHighlightColor: Int?,
	pixelPosition: Int,
	inChorusSection: Boolean,
	scrollTimes: Pair<Long, Long>,
	private val chordMap: ChordMap?,
	songLoadCancelEvent: SongLoadCancelEvent? = null
) : Line(
	lineTime,
	lineDuration,
	scrollMode,
	pixelPosition,
	inChorusSection,
	scrollTimes.first,
	scrollTimes.second,
	displaySettings
) {
	private var lineTextSize: Int = 0 // font size to use, pre-measured.
	private var chordTextSize: Int = 0 // font size to use, pre-measured.
	private var chordHeight: Int = 0
	private var lyricHeight: Int = 0
	private val xSplits = mutableListOf<Int>()
	private val lineWidths = mutableListOf<Int>()
	private val chordsDrawn = mutableListOf<Boolean>()
	private val textDrawn = mutableListOf<Boolean>()
	private val pixelSplits = mutableListOf<Boolean>()
	private var lineDescenderOffset: Int = 0
	private var chordDescenderOffset: Int = 0
	private val lyricColor: Int
	private val chordColor: Int
	private val chorusHighlightColor: Int
	private val annotationColor: Int
	private val sections: List<LineSection>

	var trailingHighlightColor: Int? = null
	override val measurements: LineMeasurements

	init {
		val paint = Paint()
		lyricColor = BeatPrompter.preferences.lyricColor
		chordColor = BeatPrompter.preferences.chordColor
		chorusHighlightColor = Utils.makeHighlightColour(BeatPrompter.preferences.chorusHighlightColor)
		annotationColor = BeatPrompter.preferences.annotationColor
		// TODO: Fix this, for god's sake!
		sections = calculateSections(songLoadCancelEvent)

		if (songLoadCancelEvent?.isCancelled == true)
			throw SongLoadCancelledException()

		// we have the sections, now fit 'em
		// Start with an arbitrary size
		val longestBits = StringBuilder()
		for (section in sections) {
			if (songLoadCancelEvent?.isCancelled == true)
				throw SongLoadCancelledException()
			section.setTextFontSizeAndMeasure(paint, 100)
			section.setChordFontSizeAndMeasure(paint, 100)
			if (section.chordWidth > section.lineWidth)
				longestBits.append(section.chordText)
			else
				longestBits.append(section.lineText)
		}
		if (songLoadCancelEvent?.isCancelled == true)
			throw SongLoadCancelledException()

		val maxLongestFontSize = BeatPrompter.fontManager.getBestFontSize(
			longestBits.toString(),
			paint,
			displaySettings.minimumFontSize,
			displaySettings.maximumFontSize,
			displaySettings.screenSize.width,
			-1
		).first.toDouble()
		var textFontSize = maxLongestFontSize
		var chordFontSize = maxLongestFontSize
		var allTextSmallerThanChords: Boolean
		var allChordsSmallerThanText: Boolean
		var textExists: Boolean
		var width: Int
		do {
			allTextSmallerThanChords = true
			allChordsSmallerThanText = true
			textExists = false
			width = 0
			for (section in sections) {
				if (songLoadCancelEvent?.isCancelled == true)
					throw SongLoadCancelledException()
				textExists = textExists or (section.lineText.isNotEmpty())
				val textWidth =
					section.setTextFontSizeAndMeasure(
						paint,
						floor(textFontSize).toInt()
					)
				val chordWidth =
					section.setChordFontSizeAndMeasure(
						paint,
						floor(chordFontSize).toInt()
					)
				if (chordWidth > textWidth)
					allChordsSmallerThanText = false
				else if (textWidth > 0 && textWidth > chordWidth)
					allTextSmallerThanChords = false
				width += max(textWidth, chordWidth)
			}
			if (songLoadCancelEvent?.isCancelled == true)
				throw SongLoadCancelledException()
			if (width >= displaySettings.screenSize.width) {
				if (textFontSize >= displaySettings.minimumFontSize + 2 && chordFontSize >= displaySettings.minimumFontSize + 2) {
					textFontSize -= 2.0
					chordFontSize -= 2.0
				} else if (textFontSize >= displaySettings.minimumFontSize + 1 && chordFontSize >= displaySettings.minimumFontSize + 1) {
					textFontSize -= 1.0
					chordFontSize -= 1.0
				} else if (textFontSize > displaySettings.minimumFontSize && chordFontSize > displaySettings.minimumFontSize) {
					chordFontSize = displaySettings.minimumFontSize.toDouble()
					textFontSize = chordFontSize
				}
			}
		} while (songLoadCancelEvent?.isCancelled != true && width >= displaySettings.screenSize.width && textFontSize > displaySettings.minimumFontSize && chordFontSize > displaySettings.minimumFontSize)
		if (songLoadCancelEvent?.isCancelled == true)
			throw SongLoadCancelledException()

		do {
			var proposedLargerTextFontSize = textFontSize
			var proposedLargerChordFontSize = chordFontSize
			if (allTextSmallerThanChords && textExists && textFontSize <= Utils.MAXIMUM_FONT_SIZE - 2 && proposedLargerTextFontSize <= displaySettings.maximumFontSize - 2)
				proposedLargerTextFontSize += 2.0
			else if (allChordsSmallerThanText && chordFontSize <= Utils.MAXIMUM_FONT_SIZE - 2 && proposedLargerChordFontSize <= displaySettings.maximumFontSize - 2)
				proposedLargerChordFontSize += 2.0
			else
			// Nothing we can do. Increasing any size will make things bigger than the screen.
				break
			allTextSmallerThanChords = true
			allChordsSmallerThanText = true
			val lastWidth = width
			width = 0
			for (section in sections) {
				if (songLoadCancelEvent?.isCancelled == true)
					throw SongLoadCancelledException()
				val textWidth =
					section.setTextFontSizeAndMeasure(
						paint,
						floor(proposedLargerTextFontSize).toInt()
					)
				val chordWidth = section.setChordFontSizeAndMeasure(
					paint,
					floor(proposedLargerChordFontSize).toInt()
				)
				if (chordWidth > textWidth)
					allChordsSmallerThanText = false
				else if (textWidth > 0 && textWidth > chordWidth)
					allTextSmallerThanChords = false
				width += max(textWidth, chordWidth)
			}
			if (songLoadCancelEvent?.isCancelled == true)
				throw SongLoadCancelledException()
			// If the text still isn't wider than the screen,
			// or it IS wider than the screen, but hasn't got any wider,
			// accept the new sizes.
			if (width < displaySettings.screenSize.width || width == lastWidth) {
				textFontSize = proposedLargerTextFontSize
				chordFontSize = proposedLargerChordFontSize
			}
		} while (songLoadCancelEvent?.isCancelled != true && width < displaySettings.screenSize.width || width == lastWidth)
		if (songLoadCancelEvent?.isCancelled == true)
			throw SongLoadCancelledException()

		lineTextSize = floor(textFontSize).toInt()
		chordTextSize = floor(chordFontSize).toInt()

		chordDescenderOffset = 0
		lineDescenderOffset = 0
		var actualLineHeight = 0
		var actualLineWidth = 0
		chordHeight = 0
		lyricHeight = 0
		var highlightColor = startingHighlightColor
		for (section in sections) {
			if (songLoadCancelEvent?.isCancelled == true)
				throw SongLoadCancelledException()
			section.setTextFontSizeAndMeasure(paint, lineTextSize)
			section.setChordFontSizeAndMeasure(paint, chordTextSize)
			lineDescenderOffset = max(lineDescenderOffset, section.lineDescenderOffset)
			chordDescenderOffset = max(chordDescenderOffset, section.chordDescenderOffset)
			lyricHeight = max(lyricHeight, section.lineHeight - section.lineDescenderOffset)
			chordHeight = max(chordHeight, section.chordHeight - section.chordDescenderOffset)
			actualLineHeight = max(
				lyricHeight + chordHeight + lineDescenderOffset + chordDescenderOffset,
				actualLineHeight
			)
			actualLineWidth += max(section.lineWidth, section.chordWidth)
			highlightColor =
				section.calculateHighlightedSections(
					paint,
					lineTextSize.toFloat(),
					highlightColor
				)
		}
		trailingHighlightColor = highlightColor
		if (songLoadCancelEvent?.isCancelled == true)
			throw SongLoadCancelledException()

		// Word wrapping time!
		if (width > displaySettings.screenSize.width) {
			var bothersomeSection: LineSection?
			do {
				// Start from the first section again, but work from off the left hand edge
				// of the screen if there are already splits.
				// This is because a section could contain one enormous word that spans
				// multiple lines.
				var totalWidth = -totalXSplits
				bothersomeSection = null
				var chordsDrawn = false
				var pixelSplit = false
				var textDrawn = false
				var firstOnscreenSection: LineSection? = null
				var lastSplitWasPixelSplit = false
				if (pixelSplits.size > 0)
					lastSplitWasPixelSplit = pixelSplits[pixelSplits.size - 1]
				for (sec in sections) {
					if (songLoadCancelEvent?.isCancelled == true)
						throw SongLoadCancelledException()
					if (totalWidth > 0 && firstOnscreenSection == null)
						firstOnscreenSection = sec
					val startX = totalWidth
					if (startX <= 0 && startX + sec.lineWidth > 0)
						textDrawn = textDrawn or sec.hasText()
					if (startX <= 0 && startX + sec.chordTrimWidth > 0 && lastSplitWasPixelSplit)
						chordsDrawn = chordsDrawn or sec.hasChord()
					totalWidth += sec.width
					if (startX >= 0 && totalWidth < displaySettings.screenSize.width) {
						// this whole section fits onscreen, no problem.
						chordsDrawn = chordsDrawn or sec.hasChord()
						textDrawn = textDrawn or sec.hasText()
					} else if (totalWidth >= displaySettings.screenSize.width) {
						bothersomeSection = sec
						break
					}
				}
				if (bothersomeSection != null) {
					val previousSplit =
						if (xSplits.size > 0) xSplits[xSplits.size - 1] else displaySettings.screenSize.width
					val leftoverSpaceOnPreviousLine = displaySettings.screenSize.width - previousSplit
					val widthWithoutBothersomeSection = totalWidth - bothersomeSection.width
					var xSplit = 0
					var lineWidth = 0
					val sectionChordOnscreen =
						bothersomeSection.hasChord() && (widthWithoutBothersomeSection >= 0 || widthWithoutBothersomeSection + bothersomeSection.chordTrimWidth > leftoverSpaceOnPreviousLine)
					val sectionTextOnscreen =
						bothersomeSection.hasText() && (widthWithoutBothersomeSection >= 0 || widthWithoutBothersomeSection + bothersomeSection.lineWidth > 0)
					// Find the last word that fits onscreen.
					var bits = Utils.splitText(bothersomeSection.lineText)
					val wordCount = Utils.countWords(bits)
					var splitOnLetter = wordCount <= 1
					if (!splitOnLetter) {
						var f = wordCount - 1
						while (f >= 1 && songLoadCancelEvent?.isCancelled != true) {
							val tryThisWithWhitespace = Utils.stitchBits(bits, f)
							val tryThis = tryThisWithWhitespace.trim()
							val tryThisWidth =
								BeatPrompter.fontManager.getStringWidth(
									paint,
									tryThis,
									lineTextSize.toFloat()
								).first
							var tryThisWithWhitespaceWidth = tryThisWidth
							if (tryThisWithWhitespace.length > tryThis.length)
								tryThisWithWhitespaceWidth = BeatPrompter.fontManager.getStringWidth(
									paint,
									tryThisWithWhitespace,
									lineTextSize.toFloat()
								).first
							if (tryThisWidth >= bothersomeSection.chordTrimWidth || bothersomeSection.chordTrimWidth + widthWithoutBothersomeSection < displaySettings.screenSize.width) {
								val possibleSplitPoint = widthWithoutBothersomeSection + tryThisWidth
								val possibleSplitPointWithWhitespace =
									widthWithoutBothersomeSection + tryThisWithWhitespaceWidth
								if (possibleSplitPoint <= 0) {
									// We're back where we started. The word we've found must be huge.
									// Let's split on letter.
									splitOnLetter = true
									break
								} else if (possibleSplitPoint < displaySettings.screenSize.width) {
									// We have a winner!
									if (bothersomeSection.chordDrawLine == -1)
										bothersomeSection.chordDrawLine = xSplits.size
									chordsDrawn = chordsDrawn or sectionChordOnscreen
									textDrawn = textDrawn or sectionTextOnscreen
									xSplit = possibleSplitPointWithWhitespace
									lineWidth = xSplit
									if (tryThisWidth < bothersomeSection.chordTrimWidth && bothersomeSection.chordTrimWidth + widthWithoutBothersomeSection < displaySettings.screenSize.width)
										lineWidth = bothersomeSection.chordTrimWidth + widthWithoutBothersomeSection
									break
								}
							} else {
								// Can we split on the section?
								if (widthWithoutBothersomeSection > 0) {
									xSplit = widthWithoutBothersomeSection
									lineWidth = xSplit
								} else {
									// No? Have to split to pixel
									chordsDrawn = chordsDrawn or sectionChordOnscreen
									textDrawn = textDrawn or sectionTextOnscreen
									xSplit = displaySettings.screenSize.width
									lineWidth = xSplit
									pixelSplit = true
								}
								break
							}
							--f
						}
						// Not even just the first word fits.
						if (xSplit == 0) {
							if (firstOnscreenSection == null || firstOnscreenSection == bothersomeSection) {
								// the current section starts before the left margin.
								// we must be in the middle of a very big word.
								// OR
								// This is the first section, and the first word doesn't fit.
								splitOnLetter = true
							} else {
								// This is the second or subsequent section in this line.
								// Safe to break on the section.
								xSplit = widthWithoutBothersomeSection
								lineWidth = xSplit
							}
						}
					}
					if (splitOnLetter) {
						// There is only one word in this section, or there are multiple words
						// but the first one is enormous.
						if (firstOnscreenSection == null) {
							// This section starts BEFORE the left margin, so we have to split on a letter.
							bits = bothersomeSection.lineText.characters()
							if (bits.size > 1) {
								var f = bits.size - 1
								while (f >= 1 && songLoadCancelEvent?.isCancelled != true) {
									val tryThis = Utils.stitchBits(bits, f)
									val tryThisWidth =
										BeatPrompter.fontManager.getStringWidth(
											paint,
											tryThis,
											lineTextSize.toFloat()
										).first
									if (tryThisWidth >= bothersomeSection.chordTrimWidth) {
										val possibleSplitPoint = widthWithoutBothersomeSection + tryThisWidth
										if (possibleSplitPoint <= 0) {
											// We're back where we started. The letter we've found must be huge!
											// Just have to split on pixel.
											pixelSplit = true
											textDrawn = textDrawn or sectionTextOnscreen
											chordsDrawn = chordsDrawn or sectionChordOnscreen
											xSplit = displaySettings.screenSize.width
											lineWidth = xSplit
											break
										} else if (possibleSplitPoint < displaySettings.screenSize.width) {
											// We have a winner!
											textDrawn = textDrawn or sectionTextOnscreen
											chordsDrawn = chordsDrawn or sectionChordOnscreen
											if (bothersomeSection.chordDrawLine == -1)
												bothersomeSection.chordDrawLine = xSplits.size
											xSplit = possibleSplitPoint
											lineWidth = xSplit
											break
										}
									} else {
										// We're not going to split the chord, and there's only one section,
										// so we can't split on that. Just going to have to split to the pixel.
										chordsDrawn = chordsDrawn or sectionChordOnscreen
										textDrawn = textDrawn or sectionTextOnscreen
										xSplit = displaySettings.screenSize.width
										lineWidth = xSplit
										pixelSplit = true
										break
									}
									--f
								}
							} else {
								// There is no text to split. Just going to have to split to the pixel.
								chordsDrawn = chordsDrawn or sectionChordOnscreen
								textDrawn = textDrawn or sectionTextOnscreen
								xSplit = displaySettings.screenSize.width
								lineWidth = xSplit
								pixelSplit = true
							}
						} else {
							// Split on the section.
							xSplit = widthWithoutBothersomeSection
							lineWidth = xSplit
						}
					}
					if (xSplit > 0)
						xSplits.add(xSplit)
					if (lineWidth > 0)
						lineWidths.add(lineWidth)
				}
				this.textDrawn.add(textDrawn)
				pixelSplits.add(pixelSplit)
				this.chordsDrawn.add(chordsDrawn)
			} while (songLoadCancelEvent?.isCancelled != true && bothersomeSection != null)
			if (songLoadCancelEvent?.isCancelled == true)
				throw SongLoadCancelledException()
		}

		val lines = xSplits.size + 1
		val graphicHeights = mutableListOf<Int>()
		if (lines == 1)
			graphicHeights.add(actualLineHeight)
		else {
			// If we are splitting the line over multiple lines, then the height will increase.
			// If the height is too much for the screen, then we have to X scroll.
			var totalLineHeight = 0
			repeat(lines) {
				val thisLineHeight =
					actualLineHeight - if (chordsDrawn[it]) 0 else chordHeight + chordDescenderOffset - if (textDrawn[it]) 0 else lyricHeight + lineDescenderOffset
				totalLineHeight += thisLineHeight
				graphicHeights.add(thisLineHeight)
			}
			actualLineHeight = totalLineHeight
			actualLineWidth = calculateWidestLineWidth(actualLineWidth)
		}

		measurements = LineMeasurements(
			lines,
			actualLineWidth,
			actualLineHeight,
			graphicHeights.toIntArray(),
			this.lineTime,
			this.lineDuration,
			scrollTimes.first,
			scrollMode
		)
	}

	private val totalXSplits: Int
		get() = xSplits.sum()

	private fun calculateSections(songLoadCancelEvent: SongLoadCancelEvent? = null): List<LineSection> {
		val sections = mutableListOf<LineSection>()
		var chordPositionStart = 0
		var chordTagIndex = -1
		run {
			val nonChordTags = tags.filter { it !is ChordTag }
			val chordTags = tags.filterIsInstance<ChordTag>()

			while (chordTagIndex < chordTags.size && songLoadCancelEvent?.isCancelled != true) {
				// If we're at the last chord, capture all tags from here to the end.
				val isLastChord = chordTagIndex == chordTags.size - 1
				val chordPositionEnd =
					if (!isLastChord)
						min(chordTags[chordTagIndex + 1].position, text.length)
					else
						text.length
				val tagPositionEnd = if (!isLastChord)
					chordPositionEnd
				else
					Int.MAX_VALUE
				// mText could have been "..." which would be turned into ""
				if (chordTagIndex != -1)
					chordPositionStart = chordTags[chordTagIndex].position
				chordPositionStart = min(text.length, chordPositionStart)
				val linePart = text.substring(chordPositionStart, chordPositionEnd)
				var chordText = ""
				var trueChord = false
				if (chordTagIndex != -1) {
					val chordTag = chordTags[chordTagIndex]
					chordText = chordTag.name
					chordText = chordMap?.getChordDisplayString(chordText) ?: chordText
					trueChord = Chord.isChord(chordText)
					// Stick a couple of spaces on each chord, apart from the last one.
					// This is so they don't appear right beside each other.
					if (!isLastChord)
						chordText += "  "
				}
				val otherTags = mutableListOf<Tag>()
				for (tag in nonChordTags)
					if (tag.position in chordPositionStart..tagPositionEnd)
						otherTags.add(tag)
				if (linePart.isNotEmpty() || chordText.isNotEmpty()) {
					val section = LineSection(linePart, chordText, trueChord, chordPositionStart, otherTags)
					sections.add(section)
				}
				chordTagIndex++
			}
			return sections
		}
	}

	private fun calculateWidestLineWidth(vTotalLineWidth: Int): Int =
		max(vTotalLineWidth - totalXSplits, lineWidths.maxOrNull() ?: 0)

	override fun renderGraphics(paint: Paint) {
		val backgroundColor = if (isInChorusSection) chorusHighlightColor else 0x0000ffff
		repeat(measurements.lines) { lineNumber ->
			val graphic = graphics[lineNumber]
			val canvas = canvasses[lineNumber]
			if (graphic.lastDrawnLine !== this) {
				val chordsDrawn = if (chordsDrawn.size > lineNumber) chordsDrawn[lineNumber] else true
				val thisLineHeight = measurements.graphicHeights[lineNumber]
				var currentX = 0
				var g = 0
				while (g < xSplits.size && g < lineNumber) {
					currentX -= xSplits[g]
					++g
				}
				BeatPrompter.fontManager.setTypeface(paint)
				canvas.drawColor(backgroundColor, PorterDuff.Mode.SRC) // Fill with transparency.
				val xSplit = if (xSplits.size > lineNumber) xSplits[lineNumber] else Integer.MAX_VALUE
				for (i in sections.indices) {
					val section = sections[i]
					if (currentX < xSplit) {
						val width = section.width
						if (currentX + width > 0) {
							if (chordsDrawn && (section.chordDrawLine == lineNumber || section.chordDrawLine == -1) && section.chordText.trim()
									.isNotEmpty()
							) {
								paint.color = if (section.isTrueChord) chordColor else annotationColor
								paint.textSize = chordTextSize * Utils.FONT_SCALING
								paint.flags = Paint.ANTI_ALIAS_FLAG
								canvas.drawText(
									section.chordText,
									currentX.toFloat(),
									chordHeight.toFloat(),
									paint
								)
							}
							canvas.save()
							if (xSplit != Integer.MAX_VALUE)
								canvas.clipRect(0, 0, xSplit, thisLineHeight)
							if (section.lineText.trim().isNotEmpty()) {
								paint.color = lyricColor
								paint.textSize = lineTextSize * Utils.FONT_SCALING
								paint.flags = Paint.ANTI_ALIAS_FLAG
								canvas.drawText(
									section.lineText,
									currentX.toFloat(),
									((if (chordsDrawn) chordHeight + chordDescenderOffset else 0) + lyricHeight).toFloat(),
									paint
								)
							}
							for (j in 0 until section.highlightingRectangles.size) {
								val highlightingRectangle = section.highlightingRectangles[j]
								val (left, _, right, _, color) = highlightingRectangle
								paint.color = color
								canvas.drawRect(
									Rect(
										left + currentX,
										if (chordsDrawn) chordHeight + chordDescenderOffset else 0,
										right + currentX,
										(if (chordsDrawn) chordHeight + chordDescenderOffset else 0) + lyricHeight + lineDescenderOffset
									), paint
								)
							}
							canvas.restore()
						}
						currentX += width
					}
				}
				graphic.lastDrawnLine = this
			}
		}
	}
}