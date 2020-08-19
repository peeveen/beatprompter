package com.stevenfrew.beatprompter.song.line

import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.cache.parse.tag.Tag
import com.stevenfrew.beatprompter.cache.parse.tag.song.ChordTag
import com.stevenfrew.beatprompter.graphics.DisplaySettings
import com.stevenfrew.beatprompter.graphics.ScreenString
import com.stevenfrew.beatprompter.song.ScrollingMode
import com.stevenfrew.beatprompter.song.load.SongLoadCancelEvent
import com.stevenfrew.beatprompter.song.load.SongLoadCancelledException
import com.stevenfrew.beatprompter.util.Utils
import com.stevenfrew.beatprompter.util.characters
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class TextLine internal constructor(private val mText: String,
                                    private val mTags: List<Tag>,
                                    lineTime: Long,
                                    lineDuration: Long,
                                    scrollMode: ScrollingMode,
                                    displaySettings: DisplaySettings,
                                    startingHighlightColor: Int?,
                                    pixelPosition: Int,
                                    inChorusSection: Boolean,
                                    scrollTimes: Pair<Long, Long>,
                                    songLoadCancelEvent: SongLoadCancelEvent)
    : Line(lineTime,
        lineDuration,
        scrollMode,
        pixelPosition,
        inChorusSection,
        scrollTimes.first,
        scrollTimes.second,
        displaySettings) {
    private var mLineTextSize: Int = 0 // font size to use, pre-measured.
    private var mChordTextSize: Int = 0 // font size to use, pre-measured.
    private var mChordHeight: Int = 0
    private var mLyricHeight: Int = 0
    private val mFont = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    private val mXSplits = mutableListOf<Int>()
    private val mLineWidths = mutableListOf<Int>()
    private val mChordsDrawn = mutableListOf<Boolean>()
    private val mTextDrawn = mutableListOf<Boolean>()
    private val mPixelSplits = mutableListOf<Boolean>()
    private var mLineDescenderOffset: Int = 0
    private var mChordDescenderOffset: Int = 0
    private val mLyricColor: Int
    private val mChordColor: Int
    private val mChorusHighlightColor: Int
    private val mAnnotationColor: Int
    private val mSections: List<LineSection>
    var mTrailingHighlightColor: Int? = null
    override val mMeasurements: LineMeasurements

    init {
        val paint = Paint()
        mLyricColor = Preferences.lyricColor
        mChordColor = Preferences.chordColor
        mChorusHighlightColor = Utils.makeHighlightColour(Preferences.chorusHighlightColor)
        mAnnotationColor = Preferences.annotationColor
        // TODO: Fix this, for god's sake!
        mSections = calculateSections(songLoadCancelEvent)

        if (songLoadCancelEvent.isCancelled)
            throw SongLoadCancelledException()

        // we have the sections, now fit 'em
        // Start with an arbitrary size
        val longestBits = StringBuilder()
        for (section in mSections) {
            if (songLoadCancelEvent.isCancelled)
                throw SongLoadCancelledException()
            section.setTextFontSizeAndMeasure(paint, 100, mFont)
            section.setChordFontSizeAndMeasure(paint, 100, mFont)
            if (section.mChordWidth > section.mLineWidth)
                longestBits.append(section.mChordText)
            else
                longestBits.append(section.mLineText)
        }
        if (songLoadCancelEvent.isCancelled)
            throw SongLoadCancelledException()

        val maxLongestFontSize = ScreenString.getBestFontSize(longestBits.toString(), paint, displaySettings.mMinFontSize, displaySettings.mMaxFontSize, displaySettings.mScreenSize.width(), -1, mFont).toDouble()
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
            for (section in mSections) {
                if (songLoadCancelEvent.isCancelled)
                    throw SongLoadCancelledException()
                textExists = textExists or (section.mLineText.isNotEmpty())
                val textWidth = section.setTextFontSizeAndMeasure(paint, floor(textFontSize).toInt(), mFont)
                val chordWidth = section.setChordFontSizeAndMeasure(paint, floor(chordFontSize).toInt(), mFont)
                if (chordWidth > textWidth)
                    allChordsSmallerThanText = false
                else if (textWidth > 0 && textWidth > chordWidth)
                    allTextSmallerThanChords = false
                width += max(textWidth, chordWidth)
            }
            if (songLoadCancelEvent.isCancelled)
                throw SongLoadCancelledException()
            if (width >= displaySettings.mScreenSize.width()) {
                if (textFontSize >= displaySettings.mMinFontSize + 2 && chordFontSize >= displaySettings.mMinFontSize + 2) {
                    textFontSize -= 2.0
                    chordFontSize -= 2.0
                } else if (textFontSize >= displaySettings.mMinFontSize + 1 && chordFontSize >= displaySettings.mMinFontSize + 1) {
                    textFontSize -= 1.0
                    chordFontSize -= 1.0
                } else if (textFontSize > displaySettings.mMinFontSize && chordFontSize > displaySettings.mMinFontSize) {
                    chordFontSize = displaySettings.mMinFontSize.toDouble()
                    textFontSize = chordFontSize
                }
            }
        } while (!songLoadCancelEvent.isCancelled && width >= displaySettings.mScreenSize.width() && textFontSize > displaySettings.mMinFontSize && chordFontSize > displaySettings.mMinFontSize)
        if (songLoadCancelEvent.isCancelled)
            throw SongLoadCancelledException()

        do {
            var proposedLargerTextFontSize = textFontSize
            var proposedLargerChordFontSize = chordFontSize
            if (allTextSmallerThanChords && textExists && textFontSize <= Utils.MAXIMUM_FONT_SIZE - 2 && proposedLargerTextFontSize <= displaySettings.mMaxFontSize - 2)
                proposedLargerTextFontSize += 2.0
            else if (allChordsSmallerThanText && chordFontSize <= Utils.MAXIMUM_FONT_SIZE - 2 && proposedLargerChordFontSize <= displaySettings.mMaxFontSize - 2)
                proposedLargerChordFontSize += 2.0
            else
            // Nothing we can do. Increasing any size will make things bigger than the screen.
                break
            allTextSmallerThanChords = true
            allChordsSmallerThanText = true
            width = 0
            for (section in mSections) {
                if (songLoadCancelEvent.isCancelled)
                    throw SongLoadCancelledException()
                val textWidth = section.setTextFontSizeAndMeasure(paint, floor(proposedLargerTextFontSize).toInt(), mFont)
                val chordWidth = section.setChordFontSizeAndMeasure(paint, floor(proposedLargerChordFontSize).toInt(), mFont)
                if (chordWidth > textWidth)
                    allChordsSmallerThanText = false
                else if (textWidth > 0 && textWidth > chordWidth)
                    allTextSmallerThanChords = false
                width += max(textWidth, chordWidth)
            }
            if (songLoadCancelEvent.isCancelled)
                throw SongLoadCancelledException()
            // If the text still isn't wider than the screen,
            // or it IS wider than the screen, but hasn't got any wider,
            // accept the new sizes.
            if (width < displaySettings.mScreenSize.width() || width == width) {
                textFontSize = proposedLargerTextFontSize
                chordFontSize = proposedLargerChordFontSize
            }
        } while (!songLoadCancelEvent.isCancelled && width < displaySettings.mScreenSize.width() || width == width)
        if (songLoadCancelEvent.isCancelled)
            throw SongLoadCancelledException()

        mLineTextSize = floor(textFontSize).toInt()
        mChordTextSize = floor(chordFontSize).toInt()

        mChordDescenderOffset = 0
        mLineDescenderOffset = mChordDescenderOffset
        var actualLineHeight = 0
        var actualLineWidth = 0
        mChordHeight = 0
        mLyricHeight = mChordHeight
        var highlightColor = startingHighlightColor
        for (section in mSections) {
            if (songLoadCancelEvent.isCancelled)
                throw SongLoadCancelledException()
            section.setTextFontSizeAndMeasure(paint, mLineTextSize, mFont)
            section.setChordFontSizeAndMeasure(paint, mChordTextSize, mFont)
            mLineDescenderOffset = max(mLineDescenderOffset, section.mLineDescenderOffset)
            mChordDescenderOffset = max(mChordDescenderOffset, section.mChordDescenderOffset)
            mLyricHeight = max(mLyricHeight, section.mLineHeight - section.mLineDescenderOffset)
            mChordHeight = max(mChordHeight, section.mChordHeight - section.mChordDescenderOffset)
            actualLineHeight = max(mLyricHeight + mChordHeight + mLineDescenderOffset + mChordDescenderOffset, actualLineHeight)
            actualLineWidth += max(section.mLineWidth, section.mChordWidth)
            highlightColor = section.calculateHighlightedSections(paint, mLineTextSize.toFloat(), mFont, highlightColor)
        }
        mTrailingHighlightColor = highlightColor
        if (songLoadCancelEvent.isCancelled)
            throw SongLoadCancelledException()

        // Word wrapping time!
        if (width > displaySettings.mScreenSize.width()) {
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
                if (mPixelSplits.size > 0)
                    lastSplitWasPixelSplit = mPixelSplits[mPixelSplits.size - 1]
                for (sec in mSections) {
                    if (songLoadCancelEvent.isCancelled)
                        throw SongLoadCancelledException()
                    if (totalWidth > 0 && firstOnscreenSection == null)
                        firstOnscreenSection = sec
                    val startX = totalWidth
                    if (startX <= 0 && startX + sec.mLineWidth > 0)
                        textDrawn = textDrawn or sec.hasText()
                    if (startX <= 0 && startX + sec.mChordTrimWidth > 0 && lastSplitWasPixelSplit)
                        chordsDrawn = chordsDrawn or sec.hasChord()
                    totalWidth += sec.width
                    if (startX >= 0 && totalWidth < displaySettings.mScreenSize.width()) {
                        // this whole section fits onscreen, no problem.
                        chordsDrawn = chordsDrawn or sec.hasChord()
                        textDrawn = textDrawn or sec.hasText()
                    } else if (totalWidth >= displaySettings.mScreenSize.width()) {
                        bothersomeSection = sec
                        break
                    }
                }
                if (bothersomeSection != null) {
                    val previousSplit = if (mXSplits.size > 0) mXSplits[mXSplits.size - 1] else displaySettings.mScreenSize.width()
                    val leftoverSpaceOnPreviousLine = displaySettings.mScreenSize.width() - previousSplit
                    val widthWithoutBothersomeSection = totalWidth - bothersomeSection.width
                    var xSplit = 0
                    var lineWidth = 0
                    val sectionChordOnscreen = bothersomeSection.hasChord() && (widthWithoutBothersomeSection >= 0 || widthWithoutBothersomeSection + bothersomeSection.mChordTrimWidth > leftoverSpaceOnPreviousLine)
                    val sectionTextOnscreen = bothersomeSection.hasText() && (widthWithoutBothersomeSection >= 0 || widthWithoutBothersomeSection + bothersomeSection.mLineWidth > 0)
                    // Find the last word that fits onscreen.
                    var bits = Utils.splitText(bothersomeSection.mLineText)
                    val wordCount = Utils.countWords(bits)
                    var splitOnLetter = wordCount <= 1
                    if (!splitOnLetter) {
                        var f = wordCount - 1
                        while (f >= 1 && !songLoadCancelEvent.isCancelled) {
                            val tryThisWithWhitespace = Utils.stitchBits(bits, f)
                            val tryThis = tryThisWithWhitespace.trim()
                            val tryThisWidth = ScreenString.getStringWidth(paint, tryThis, mFont, mLineTextSize.toFloat())
                            var tryThisWithWhitespaceWidth = tryThisWidth
                            if (tryThisWithWhitespace.length > tryThis.length)
                                tryThisWithWhitespaceWidth = ScreenString.getStringWidth(paint, tryThisWithWhitespace, mFont, mLineTextSize.toFloat())
                            if (tryThisWidth >= bothersomeSection.mChordTrimWidth || tryThisWidth < bothersomeSection.mChordTrimWidth && bothersomeSection.mChordTrimWidth + widthWithoutBothersomeSection < displaySettings.mScreenSize.width()) {
                                val possibleSplitPoint = widthWithoutBothersomeSection + tryThisWidth
                                val possibleSplitPointWithWhitespace = widthWithoutBothersomeSection + tryThisWithWhitespaceWidth
                                if (possibleSplitPoint <= 0) {
                                    // We're back where we started. The word we've found must be huge.
                                    // Let's split on letter.
                                    splitOnLetter = true
                                    break
                                } else if (possibleSplitPoint < displaySettings.mScreenSize.width()) {
                                    // We have a winner!
                                    if (bothersomeSection.mChordDrawLine == -1)
                                        bothersomeSection.mChordDrawLine = mXSplits.size
                                    chordsDrawn = chordsDrawn or sectionChordOnscreen
                                    textDrawn = textDrawn or sectionTextOnscreen
                                    xSplit = possibleSplitPointWithWhitespace
                                    lineWidth = xSplit
                                    if (tryThisWidth < bothersomeSection.mChordTrimWidth && bothersomeSection.mChordTrimWidth + widthWithoutBothersomeSection < displaySettings.mScreenSize.width())
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
                                    xSplit = displaySettings.mScreenSize.width()
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
                            bits = bothersomeSection.mLineText.characters()
                            if (bits.size > 1) {
                                var f = bits.size - 1
                                while (f >= 1 && !songLoadCancelEvent.isCancelled) {
                                    val tryThis = Utils.stitchBits(bits, f)
                                    val tryThisWidth = ScreenString.getStringWidth(paint, tryThis, mFont, mLineTextSize.toFloat())
                                    if (tryThisWidth >= bothersomeSection.mChordTrimWidth) {
                                        val possibleSplitPoint = widthWithoutBothersomeSection + tryThisWidth
                                        if (possibleSplitPoint <= 0) {
                                            // We're back where we started. The letter we've found must be huge!
                                            // Just have to split on pixel.
                                            pixelSplit = true
                                            textDrawn = textDrawn or sectionTextOnscreen
                                            chordsDrawn = chordsDrawn or sectionChordOnscreen
                                            xSplit = displaySettings.mScreenSize.width()
                                            lineWidth = xSplit
                                            break
                                        } else if (possibleSplitPoint < displaySettings.mScreenSize.width()) {
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
                                        xSplit = displaySettings.mScreenSize.width()
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
                                xSplit = displaySettings.mScreenSize.width()
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
            } while (!songLoadCancelEvent.isCancelled && bothersomeSection != null)
            if (songLoadCancelEvent.isCancelled)
                throw SongLoadCancelledException()
        }

        val lines = mXSplits.size + 1
        val graphicHeights = mutableListOf<Int>()
        if (lines == 1)
            graphicHeights.add(actualLineHeight)
        else {
            // If we are splitting the line over multiple lines, then the height will increase.
            // If the height is too much for the screen, then we have to X scroll.
            var totalLineHeight = 0
            repeat(lines) {
                val thisLineHeight = actualLineHeight - if (mChordsDrawn[it]) 0 else mChordHeight + mChordDescenderOffset - if (mTextDrawn[it]) 0 else mLyricHeight + mLineDescenderOffset
                totalLineHeight += thisLineHeight
                graphicHeights.add(thisLineHeight)
            }
            actualLineHeight = totalLineHeight
            actualLineWidth = calculateWidestLineWidth(actualLineWidth)
        }

        mMeasurements = LineMeasurements(lines, actualLineWidth, actualLineHeight, graphicHeights.toIntArray(), mLineTime, mLineDuration, scrollTimes.first, scrollMode)
    }

    private val totalXSplits: Int
        get() {
            return mXSplits.sum()
        }

    private fun calculateSections(songLoadCancelEvent: SongLoadCancelEvent): List<LineSection> {
        val sections = mutableListOf<LineSection>()
        var chordPositionStart = 0
        var chordTagIndex = -1
        run {
            val nonChordTags = mTags.filter { it !is ChordTag }
            val chordTags = mTags.filterIsInstance<ChordTag>()

            while (chordTagIndex < chordTags.size && !songLoadCancelEvent.isCancelled) {
                // If we're at the last chord, capture all tags from here to the end.
                val isLastChord = chordTagIndex == chordTags.size - 1
                val chordPositionEnd =
                        if (!isLastChord)
                            min(chordTags[chordTagIndex + 1].mPosition, mText.length)
                        else
                            mText.length
                val tagPositionEnd = if (!isLastChord)
                    chordPositionEnd
                else
                    Int.MAX_VALUE
                // mText could have been "..." which would be turned into ""
                if (chordTagIndex != -1)
                    chordPositionStart = chordTags[chordTagIndex].mPosition
                chordPositionStart = min(mText.length, chordPositionStart)
                val linePart = mText.substring(chordPositionStart, chordPositionEnd)
                var chordText = ""
                var trueChord = false
                if (chordTagIndex != -1) {
                    val chordTag = chordTags[chordTagIndex]
                    trueChord = chordTag.mValidChord
                    chordText = chordTag.mName
                    // Stick a couple of spaces on each chord, apart from the last one.
                    // This is so they don't appear right beside each other.
                    if (!isLastChord)
                        chordText += "  "
                }
                val otherTags = mutableListOf<Tag>()
                for (tag in nonChordTags)
                    if (tag.mPosition in chordPositionStart..tagPositionEnd)
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

    private fun calculateWidestLineWidth(vTotalLineWidth: Int): Int {
        return max(vTotalLineWidth - totalXSplits, mLineWidths.maxOrNull() ?: 0)
    }

    override fun renderGraphics(paint: Paint) {
        val backgroundColor = if (mInChorusSection) mChorusHighlightColor else 0x0000ffff
        repeat(mMeasurements.mLines) { lineNumber ->
            val graphic = mGraphics[lineNumber]
            val canvas = mCanvasses[lineNumber]
            if (graphic.mLastDrawnLine !== this) {
                val chordsDrawn = if (mChordsDrawn.size > lineNumber) mChordsDrawn[lineNumber] else true
                val thisLineHeight = mMeasurements.mGraphicHeights[lineNumber]
                var currentX = 0
                var g = 0
                while (g < mXSplits.size && g < lineNumber) {
                    currentX -= mXSplits[g]
                    ++g
                }
                paint.typeface = mFont
                canvas.drawColor(backgroundColor, PorterDuff.Mode.SRC) // Fill with transparency.
                val xSplit = if (mXSplits.size > lineNumber) mXSplits[lineNumber] else Integer.MAX_VALUE
                for (i in mSections.indices) {
                    val section = mSections[i]
                    if (currentX < xSplit) {
                        val width = section.width
                        if (currentX + width > 0) {
                            if (chordsDrawn && (section.mChordDrawLine == lineNumber || section.mChordDrawLine == -1) && currentX < xSplit && section.mChordText.trim().isNotEmpty()) {
                                paint.color = if (section.mTrueChord) mChordColor else mAnnotationColor
                                paint.textSize = mChordTextSize * Utils.FONT_SCALING
                                paint.flags = Paint.ANTI_ALIAS_FLAG
                                canvas.drawText(section.mChordText, currentX.toFloat(), mChordHeight.toFloat(), paint)
                            }
                            canvas.save()
                            if (xSplit != Integer.MAX_VALUE)
                                canvas.clipRect(0, 0, xSplit, thisLineHeight)
                            if (section.mLineText.trim().isNotEmpty()) {
                                paint.color = mLyricColor
                                paint.textSize = mLineTextSize * Utils.FONT_SCALING
                                paint.flags = Paint.ANTI_ALIAS_FLAG
                                canvas.drawText(section.mLineText, currentX.toFloat(), ((if (chordsDrawn) mChordHeight + mChordDescenderOffset else 0) + mLyricHeight).toFloat(), paint)
                            }
                            for (j in 0 until section.mHighlightingRectangles.size) {
                                val highlightingRectangle = section.mHighlightingRectangles[j]
                                val (left, _, right, _, color) = highlightingRectangle
                                paint.color = color
                                canvas.drawRect(Rect(left + currentX, if (chordsDrawn) mChordHeight + mChordDescenderOffset else 0, right + currentX, (if (chordsDrawn) mChordHeight + mChordDescenderOffset else 0) + mLyricHeight + mLineDescenderOffset), paint)
                            }
                            canvas.restore()
                        }
                        currentX += width
                    }
                }
                graphic.mLastDrawnLine = this
            }
        }
    }
}