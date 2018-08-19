package com.stevenfrew.beatprompter

import android.graphics.*
import com.stevenfrew.beatprompter.cache.parse.FileParseError
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ChordTag
import com.stevenfrew.beatprompter.songload.CancelEvent
import com.stevenfrew.beatprompter.event.ColorEvent

class TextLine internal constructor(lineTime: Long, lineDuration:Long, private val mText: String, private val mLineTags: Collection<Tag>, lastColor: ColorEvent, beatInfo:BeatInfo) : Line(lineTime,lineDuration, lastColor, beatInfo) {
    private var mLineTextSize: Int = 0 // font size to use, pre-measured.
    private var mChordTextSize: Int = 0 // font size to use, pre-measured.
    private var mChordHeight: Int = 0
    private var mLyricHeight: Int = 0
    private var mFont: Typeface? = null
    private var mFirstLineSection: LineSection? = null
    private val mXSplits = mutableListOf<Int>()
    private val mLineWidths = mutableListOf<Int>()
    private val mChordsDrawn = mutableListOf<Boolean>()
    private val mTextDrawn = mutableListOf<Boolean>()
    private val mPixelSplits = mutableListOf<Boolean>()
    private var mLineDescenderOffset: Int = 0
    private var mChordDescenderOffset: Int = 0

    private val totalXSplits: Int
        get() {
            return mXSplits.sum()
        }

    private fun calculateSections(cancelEvent:CancelEvent):LineSectionList
    {
        val sections= LineSectionList()
        var chordPositionStart = 0
        var chordTagIndex = -1
        run {
            val nonChordTags = mLineTags.filter { it !is ChordTag }
            val chordTags = mLineTags.filterIsInstance<ChordTag>()

            while (chordTagIndex < chordTags.size && !cancelEvent.isCancelled) {
                var chordPositionEnd = mText.length
                if (chordTagIndex < chordTags.size - 1)
                    chordPositionEnd = chordTags[chordTagIndex + 1].position
                // mText could have been "..." which would be turned into ""
                if (chordTagIndex != -1)
                    chordPositionStart = chordTags[chordTagIndex].position
                if (chordPositionEnd > mText.length)
                    chordPositionEnd = mText.length
                if (chordPositionStart > mText.length)
                    chordPositionStart = mText.length
                val linePart = mText.substring(chordPositionStart, chordPositionEnd)
                var chordText = ""
                var trueChord = false
                if (chordTagIndex != -1) {
                    val chordTag = chordTags[chordTagIndex]
                    trueChord = chordTag.isValidChord()
                    chordText = chordTag.mName
                    // Stick a couple of spaces on each chord, apart from the last one.
                    // This is so they don't appear right beside each other.
                    if (chordTagIndex < chordTags.size - 1) {
                        chordText += "  "
                        // TODO: POTENTIAL BREAKING CHANGE!
                        //chordTag.mName = chordText
                    }
                }
                val otherTags = mutableListOf<Tag>()
                for (tag in nonChordTags)
                    if (tag.position in chordPositionStart..chordPositionEnd)
                        otherTags.add(tag)
                if (linePart.isNotEmpty() || chordText.isNotEmpty()) {
                    val section = LineSection(linePart, chordText, trueChord, chordPositionStart, otherTags)
                    sections.add(section)
                }
                chordTagIndex++
            }
            mFirstLineSection=sections.firstOrNull()
            return sections
        }
    }

    // TODO: Fix this, for god's sake!
    override fun doMeasurements(paint: Paint, minimumFontSize: Float, maximumFontSize: Float, screenWidth: Int, screenHeight: Int, font: Typeface, highlightColour: Int, defaultHighlightColour: Int, errors: MutableList<FileParseError>, scrollMode: ScrollingMode, cancelEvent: CancelEvent): LineMeasurements? {
        var vHighlightColour = highlightColour
        mFont = font
        val sections = calculateSections(cancelEvent)

        if (cancelEvent.isCancelled)
            return null

        // we have the sections, now fit 'em
        // Start with an arbitrary size
        val longestBits = StringBuilder()
        for (section in sections) {
            if (!cancelEvent.isCancelled)
                break
            section.setTextFontSizeAndMeasure(paint, 100, font, mColorEvent.mLyricColor)
            section.setChordFontSizeAndMeasure(paint, 100, font, mColorEvent.mLyricColor)
            if (section.mChordWidth > section.mTextWidth)
                longestBits.append(section.mChordText)
            else
                longestBits.append(section.mLineText)
        }
        if (cancelEvent.isCancelled)
            return null

        val maxLongestFontSize = ScreenString.getBestFontSize(longestBits.toString(), paint, minimumFontSize, maximumFontSize, screenWidth, -1, font).toDouble()
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
                if (cancelEvent.isCancelled)
                    break
                textExists = textExists or (section.mLineText!!.isNotEmpty())
                val textWidth = section.setTextFontSizeAndMeasure(paint, Math.floor(textFontSize).toInt(), font, mColorEvent.mLyricColor)
                val chordWidth = section.setChordFontSizeAndMeasure(paint, Math.floor(chordFontSize).toInt(), font, mColorEvent.mChordColor)
                if (chordWidth > textWidth)
                    allChordsSmallerThanText = false
                else if (textWidth > 0 && textWidth > chordWidth)
                    allTextSmallerThanChords = false
                width += Math.max(textWidth, chordWidth)
            }
            if (cancelEvent.isCancelled)
                break
            if (width >= screenWidth) {
                if (textFontSize >= minimumFontSize + 2 && chordFontSize >= minimumFontSize + 2) {
                    textFontSize -= 2.0
                    chordFontSize -= 2.0
                } else if (textFontSize >= minimumFontSize + 1 && chordFontSize >= minimumFontSize + 1) {
                    textFontSize -= 1.0
                    chordFontSize -= 1.0
                } else if (textFontSize > minimumFontSize && chordFontSize > minimumFontSize) {
                    chordFontSize = minimumFontSize.toDouble()
                    textFontSize = chordFontSize
                }
            }
        } while (!cancelEvent.isCancelled && width >= screenWidth && textFontSize > minimumFontSize && chordFontSize > minimumFontSize)
        if (cancelEvent.isCancelled)
            return null

        do {
            var proposedLargerTextFontSize = textFontSize
            var proposedLargerChordFontSize = chordFontSize
            if (allTextSmallerThanChords && textExists && textFontSize <= Utils.MAXIMUM_FONT_SIZE - 2 && proposedLargerTextFontSize <= maximumFontSize - 2)
                proposedLargerTextFontSize += 2.0
            else if (allChordsSmallerThanText && chordFontSize <= Utils.MAXIMUM_FONT_SIZE - 2 && proposedLargerChordFontSize <= maximumFontSize - 2)
                proposedLargerChordFontSize += 2.0
            else
            // Nothing we can do. Increasing any size will make things bigger than the screen.
                break
            allTextSmallerThanChords = true
            allChordsSmallerThanText = true
            width = 0
            for (section in sections) {
                if (cancelEvent.isCancelled)
                    break
                val textWidth = section.setTextFontSizeAndMeasure(paint, Math.floor(proposedLargerTextFontSize).toInt(), font, mColorEvent.mLyricColor)
                val chordWidth = section.setChordFontSizeAndMeasure(paint, Math.floor(proposedLargerChordFontSize).toInt(), font, mColorEvent.mChordColor)
                if (chordWidth > textWidth)
                    allChordsSmallerThanText = false
                else if (textWidth > 0 && textWidth > chordWidth)
                    allTextSmallerThanChords = false
                width += Math.max(textWidth, chordWidth)
            }
            if (cancelEvent.isCancelled)
                break
            // If the text still isn't wider than the screen,
            // or it IS wider than the screen, but hasn't got any wider,
            // accept the new sizes.
            if (width < screenWidth || width == width) {
                textFontSize = proposedLargerTextFontSize
                chordFontSize = proposedLargerChordFontSize
            }
        } while (!cancelEvent.isCancelled && width < screenWidth || width == width)
        if (cancelEvent.isCancelled)
            return null

        mLineTextSize = Math.floor(textFontSize).toInt()
        mChordTextSize = Math.floor(chordFontSize).toInt()

        mChordDescenderOffset = 0
        mLineDescenderOffset = mChordDescenderOffset
        var actualLineHeight = 0
        var actualLineWidth = 0
        mChordHeight = 0
        mLyricHeight = mChordHeight
        for (section in sections) {
            if (cancelEvent.isCancelled)
                break
            section.setTextFontSizeAndMeasure(paint, mLineTextSize, font, mColorEvent.mLyricColor)
            section.setChordFontSizeAndMeasure(paint, mChordTextSize, font, mColorEvent.mChordColor)
            mLineDescenderOffset = Math.max(mLineDescenderOffset, section.mLineSS!!.mDescenderOffset)
            mChordDescenderOffset = Math.max(mChordDescenderOffset, section.mChordSS!!.mDescenderOffset)
            mLyricHeight = Math.max(mLyricHeight, section.mTextHeight - section.mLineSS!!.mDescenderOffset)
            mChordHeight = Math.max(mChordHeight, section.mChordHeight - section.mChordSS!!.mDescenderOffset)
            actualLineHeight = Math.max(mLyricHeight + mChordHeight + mLineDescenderOffset + mChordDescenderOffset, actualLineHeight)
            actualLineWidth += Math.max(section.mLineSS!!.mWidth, section.mChordSS!!.mWidth)
            vHighlightColour = section.calculateHighlightedSections(paint, mLineTextSize.toFloat(), font, vHighlightColour)
        }
        if (cancelEvent.isCancelled)
            return null

        // Word wrappin' time!
        if (width > screenWidth) {
            var bothersomeSection: LineSection?
            do {
                // Start from the first section again, but work from off the lefthand edge
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
                if (mPixelSplits.size > 0)
                    lastSplitWasPixelSplit = mPixelSplits[mPixelSplits.size - 1]
                for (sec in sections) {
                    if (cancelEvent.isCancelled)
                        break
                    if (totalWidth > 0 && firstOnscreenSection == null)
                        firstOnscreenSection = sec
                    val startX = totalWidth
                    if (startX <= 0 && startX + sec.mTextWidth > 0)
                        textDrawn = textDrawn or sec.hasText()
                    if (startX <= 0 && startX + sec.mChordTrimWidth > 0 && lastSplitWasPixelSplit)
                        chordsDrawn = chordsDrawn or sec.hasChord()
                    totalWidth += sec.width
                    if (startX >= 0 && totalWidth < screenWidth) {
                        // this whole section fits onscreen, no problem.
                        chordsDrawn = chordsDrawn or sec.hasChord()
                        textDrawn = textDrawn or sec.hasText()
                    } else if (totalWidth >= screenWidth) {
                        bothersomeSection = sec
                        break
                    }
                }
                if (bothersomeSection != null) {
                    val previousSplit = if (mXSplits.size > 0) mXSplits[mXSplits.size - 1] else screenWidth
                    val leftoverSpaceOnPreviousLine = screenWidth - previousSplit
                    val widthWithoutBothersomeSection = totalWidth - bothersomeSection.width
                    var xSplit = 0
                    var lineWidth = 0
                    val sectionChordOnscreen = bothersomeSection.hasChord() && (widthWithoutBothersomeSection >= 0 || widthWithoutBothersomeSection + bothersomeSection.mChordTrimWidth > leftoverSpaceOnPreviousLine)
                    val sectionTextOnscreen = bothersomeSection.hasText() && (widthWithoutBothersomeSection >= 0 || widthWithoutBothersomeSection + bothersomeSection.mTextWidth > 0)
                    // Find the last word that fits onscreen.
                    var bits = Utils.splitText(bothersomeSection.mLineText!!)
                    val wordCount = Utils.countWords(bits)
                    var splitOnLetter = wordCount <= 1
                    if (!splitOnLetter) {
                        var f = wordCount - 1
                        while (f >= 1 && !cancelEvent.isCancelled) {
                            val tryThisWithWhitespace = Utils.stitchBits(bits, f)
                            val tryThis = tryThisWithWhitespace.trim { it <= ' ' }
                            val tryThisWidth = ScreenString.getStringWidth(paint, tryThis, font, mLineTextSize.toFloat())
                            var tryThisWithWhitespaceWidth = tryThisWidth
                            if (tryThisWithWhitespace.length > tryThis.length)
                                tryThisWithWhitespaceWidth = ScreenString.getStringWidth(paint, tryThisWithWhitespace, font, mLineTextSize.toFloat())
                            if (tryThisWidth >= bothersomeSection.mChordTrimWidth || tryThisWidth < bothersomeSection.mChordTrimWidth && bothersomeSection.mChordTrimWidth + widthWithoutBothersomeSection < screenWidth) {
                                val possibleSplitPoint = widthWithoutBothersomeSection + tryThisWidth
                                val possibleSplitPointWithWhitespace = widthWithoutBothersomeSection + tryThisWithWhitespaceWidth
                                if (possibleSplitPoint <= 0) {
                                    // We're back where we started. The word we've found must be huge.
                                    // Let's split on letter.
                                    splitOnLetter = true
                                    break
                                } else if (possibleSplitPoint < screenWidth) {
                                    // We have a winner!
                                    if (bothersomeSection.mChordDrawLine == -1)
                                        bothersomeSection.mChordDrawLine = mXSplits.size
                                    chordsDrawn = chordsDrawn or sectionChordOnscreen
                                    textDrawn = textDrawn or sectionTextOnscreen
                                    xSplit = possibleSplitPointWithWhitespace
                                    lineWidth = xSplit
                                    if (tryThisWidth < bothersomeSection.mChordTrimWidth && bothersomeSection.mChordTrimWidth + widthWithoutBothersomeSection < screenWidth)
                                        lineWidth = bothersomeSection.mChordTrimWidth + widthWithoutBothersomeSection
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
                                    xSplit = screenWidth
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
                            bits = Utils.splitIntoLetters(bothersomeSection.mLineText!!)
                            if (bits.size > 1) {
                                var f = bits.size - 1
                                while (f >= 1 && !cancelEvent.isCancelled) {
                                    val tryThis = Utils.stitchBits(bits, f)
                                    val tryThisWidth = ScreenString.getStringWidth(paint, tryThis, font, mLineTextSize.toFloat())
                                    if (tryThisWidth >= bothersomeSection.mChordTrimWidth) {
                                        val possibleSplitPoint = widthWithoutBothersomeSection + tryThisWidth
                                        if (possibleSplitPoint <= 0) {
                                            // We're back where we started. The letter we've found must be huge!
                                            // Just have to split on pixel.
                                            pixelSplit = true
                                            textDrawn = textDrawn or sectionTextOnscreen
                                            chordsDrawn = chordsDrawn or sectionChordOnscreen
                                            xSplit = screenWidth
                                            lineWidth = xSplit
                                            break
                                        } else if (possibleSplitPoint < screenWidth) {
                                            // We have a winner!
                                            textDrawn = textDrawn or sectionTextOnscreen
                                            chordsDrawn = chordsDrawn or sectionChordOnscreen
                                            if (bothersomeSection.mChordDrawLine == -1)
                                                bothersomeSection.mChordDrawLine = mXSplits.size
                                            xSplit = possibleSplitPoint
                                            lineWidth = xSplit
                                            break
                                        }
                                    } else {
                                        // We're not going to split the chord, and there's only one section,
                                        // so we can't split on that. Just going to have to split to the pixel.
                                        chordsDrawn = chordsDrawn or sectionChordOnscreen
                                        textDrawn = textDrawn or sectionTextOnscreen
                                        xSplit = screenWidth
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
                                xSplit = screenWidth
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
                        mXSplits.add(xSplit)
                    if (lineWidth > 0)
                        mLineWidths.add(lineWidth)
                }
                mTextDrawn.add(textDrawn)
                mPixelSplits.add(pixelSplit)
                mChordsDrawn.add(chordsDrawn)
            } while (!cancelEvent.isCancelled && bothersomeSection != null)
            if (cancelEvent.isCancelled)
                return null
        }

        val lines = mXSplits.size + 1
        val graphicHeights = mutableListOf<Int>()
        if (lines == 1)
            graphicHeights.add(actualLineHeight)
        else {
            // If we are splitting the line over multiple lines, then the height will increase.
            // If the height is too much for the screen, then we have to X scroll.
            var totalLineHeight = 0
            for (f in 0 until lines) {
                val thisLineHeight = actualLineHeight - if (mChordsDrawn[f]) 0 else mChordHeight + mChordDescenderOffset - if (mTextDrawn[f]) 0 else mLyricHeight + mLineDescenderOffset
                totalLineHeight += thisLineHeight
                graphicHeights.add(thisLineHeight)
            }
            actualLineHeight = totalLineHeight
            actualLineWidth = calculateWidestLineWidth(actualLineWidth)
        }

        return LineMeasurements(lines, actualLineWidth, actualLineHeight, graphicHeights.toIntArray(), vHighlightColour, mLineEvent, mNextLine, mYStartScrollTime, scrollMode)
    }

    private fun calculateWidestLineWidth(vTotalLineWidth: Int): Int {
        return Math.max(vTotalLineWidth-totalXSplits, mLineWidths.max()?:0)
    }

    override fun getGraphics(allocate: Boolean): Collection<LineGraphic> {
        for (f in 0 until mLineMeasurements!!.mLines) {
            val graphic = mGraphics[f]
            if (graphic.mLastDrawnLine !== this && allocate) {
                val paint = Paint()
                val chordsDrawn = if (mChordsDrawn.size > f) mChordsDrawn[f] else true
                val thisLineHeight = mLineMeasurements!!.mGraphicHeights[f]
                val c = Canvas(graphic.mBitmap)
                var currentX = 0
                var g = 0
                while (g < mXSplits.size && g < f) {
                    currentX -= mXSplits[g]
                    ++g
                }
                paint.typeface = mFont
                c.drawColor(0x0000ffff, PorterDuff.Mode.SRC) // Fill with transparency.
                val xSplit = if (mXSplits.size > f) mXSplits[f] else Integer.MAX_VALUE
                var section = mFirstLineSection
                while (section != null && currentX < xSplit) {
                    val width = section.width
                    if (currentX + width > 0) {
                        if (chordsDrawn && (section.mChordDrawLine == f || section.mChordDrawLine == -1) && currentX < xSplit && section.mChordText!!.trim { it <= ' ' }.isNotEmpty()) {
                            paint.color = if (section.mTrueChord) mColorEvent.mChordColor else mColorEvent.mAnnotationColor
                            paint.textSize = mChordTextSize * Utils.FONT_SCALING
                            paint.flags = Paint.ANTI_ALIAS_FLAG
                            c.drawText(section.mChordText!!, currentX.toFloat(), mChordHeight.toFloat(), paint)
                        }
                        c.save()
                        if (xSplit != Integer.MAX_VALUE)
                            c.clipRect(0, 0, xSplit, thisLineHeight)
                        if (section.mLineText!!.trim { it <= ' ' }.isNotEmpty()) {
                            paint.color = mColorEvent.mLyricColor
                            paint.textSize = mLineTextSize * Utils.FONT_SCALING
                            paint.flags = Paint.ANTI_ALIAS_FLAG
                            c.drawText(section.mLineText!!, currentX.toFloat(), ((if (chordsDrawn) mChordHeight + mChordDescenderOffset else 0) + mLyricHeight).toFloat(), paint)
                        }
                        for ((left, _, right, _, color) in section.mHighlightingRectangles) {
                            paint.color = color
                            c.drawRect(Rect(left + currentX, if (chordsDrawn) mChordHeight + mChordDescenderOffset else 0, right + currentX, (if (chordsDrawn) mChordHeight + mChordDescenderOffset else 0) + mLyricHeight + mLineDescenderOffset), paint)
                        }
                        c.restore()
                    }
                    section = section.mNextSection
                    currentX += width
                }
                graphic.mLastDrawnLine = this
            }
        }
        return mGraphics
    }
}
