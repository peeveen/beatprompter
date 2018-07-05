package com.stevenfrew.beatprompter;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;

import com.stevenfrew.beatprompter.cache.FileParseError;
import com.stevenfrew.beatprompter.cache.Tag;
import com.stevenfrew.beatprompter.event.CancelEvent;
import com.stevenfrew.beatprompter.event.ColorEvent;

import java.util.ArrayList;
import java.util.Collection;

public class TextLine extends Line
{
    private String mText;
    private int mLineTextSize; // font size to use, pre-measured.
    private int mChordTextSize; // font size to use, pre-measured.
    private int mChordHeight;
    private int mLyricHeight;
    private Typeface mFont;
    private ArrayList<Tag> mLineTags=new ArrayList<>();
    private ArrayList<Tag> mChordTags=new ArrayList<>();
    private LineSection mFirstLineSection;
    private ArrayList<Integer> mXSplits=new ArrayList<>();
    private ArrayList<Integer> mLineWidths=new ArrayList<>();
    private ArrayList<Boolean> mChordsDrawn=new ArrayList<>();
    private ArrayList<Boolean> mTextDrawn=new ArrayList<>();
    private ArrayList<Boolean> mPixelSplits=new ArrayList<>();
    private int mLineDescenderOffset;
    private int mChordDescenderOffset;

    public TextLine(String lineText, Collection<Tag> lineTags, int bars, ColorEvent lastColor, int bpb, int scrollbeat, int scrollbeatOffset, ScrollingMode scrollingMode, ArrayList<FileParseError> parseErrors)
    {
        super(lineTags,bars,lastColor,bpb,scrollbeat,scrollbeatOffset,scrollingMode,parseErrors);
        mText=lineText;
        for(Tag tag:lineTags)
            if(tag.mChordTag)
                mChordTags.add(tag);
            else
                mLineTags.add(tag);
    }

    // TODO: Fix this, for god's sake!
    public LineMeasurements doMeasurements(Paint paint, float minimumFontSize, float maximumFontSize, int screenWidth,int screenHeight,  Typeface font, int highlightColour, int defaultHighlightColour, ArrayList<FileParseError> errors, ScrollingMode scrollMode,CancelEvent cancelEvent)
    {
        mFont=font;
        ArrayList<LineSection> sections = new ArrayList<>();

        int chordPositionStart = 0;
        for (int chordTagIndex = -1; chordTagIndex < mChordTags.size() && !cancelEvent.isCancelled(); chordTagIndex++) {
            int chordPositionEnd = mText.length();
            if (chordTagIndex < mChordTags.size() - 1)
                chordPositionEnd = mChordTags.get(chordTagIndex + 1).mPosition;
            // mText could have been "..." which would be turned into ""
            if (chordTagIndex != -1)
                chordPositionStart = mChordTags.get(chordTagIndex).mPosition;
            if (chordPositionEnd > mText.length())
                chordPositionEnd = mText.length();
            if (chordPositionStart > mText.length())
                chordPositionStart = mText.length();
            String linePart = mText.substring(chordPositionStart, chordPositionEnd);
            String chordText = "";
            if (chordTagIndex != -1) {
                Tag chordTag = mChordTags.get(chordTagIndex);
                chordText = chordTag.mName;
                // Stick a couple of spaces on each chord, apart from the last one.
                // This is so they don't appear right beside each other.
                if (chordTagIndex < mChordTags.size() - 1)
                {
                    chordText += "  ";
                    chordTag.mName = chordText;
                }
            }
            ArrayList<Tag> otherTags = new ArrayList<>();
            for (Tag tag : mLineTags)
                if ((tag.mPosition <= chordPositionEnd) && (tag.mPosition >= chordPositionStart))
                    otherTags.add(tag);
            if ((linePart.length() > 0) || (chordText.length() > 0)) {
                LineSection section = new LineSection(linePart, chordText, chordPositionStart, otherTags);
                if (mFirstLineSection == null)
                    mFirstLineSection = section;
                else {
                    section.mPrevSection = sections.get(sections.size() - 1);
                    sections.get(sections.size() - 1).mNextSection = section;
                }
                sections.add(section);
            }
        }
        if(cancelEvent.isCancelled())
            return null;

        // we have the sections, now fit 'em
        // Start with an arbitrary size
        StringBuilder longestBits = new StringBuilder();
        for (LineSection section : sections) {
            if(!cancelEvent.isCancelled())
                break;
            section.setTextFontSizeAndMeasure(paint, 100, font, mColorEvent.mLyricColor);
            section.setChordFontSizeAndMeasure(paint, 100, font, mColorEvent.mLyricColor);
            if (section.mChordWidth > section.mTextWidth)
                longestBits.append(section.mChordText);
            else
                longestBits.append(section.mLineText);
        }
        if(cancelEvent.isCancelled())
            return null;

        double maxLongestFontSize = ScreenString.getBestFontSize(longestBits.toString(), paint, minimumFontSize,maximumFontSize, screenWidth, -1, font);
        double textFontSize = maxLongestFontSize;
        double chordFontSize = maxLongestFontSize;
        boolean allTextSmallerThanChords, allChordsSmallerThanText;
        boolean textExists;
        int width;
        do {
            allTextSmallerThanChords = true;
            allChordsSmallerThanText = true;
            textExists = false;
            width = 0;
            for (LineSection section : sections) {
                if(cancelEvent.isCancelled())
                    break;
                textExists |= section.mLineText.length() > 0;
                int textWidth = section.setTextFontSizeAndMeasure(paint, (int) Math.floor(textFontSize), font, mColorEvent.mLyricColor);
                int chordWidth = section.setChordFontSizeAndMeasure(paint, (int) Math.floor(chordFontSize), font, mColorEvent.mChordColor);
                if (chordWidth > textWidth)
                    allChordsSmallerThanText = false;
                else if ((textWidth > 0) && (textWidth > chordWidth))
                    allTextSmallerThanChords = false;
                width += Math.max(textWidth, chordWidth);
            }
            if(cancelEvent.isCancelled())
                break;
            if (width >= screenWidth)
            {
                if((textFontSize >= minimumFontSize + 2) && (chordFontSize >= minimumFontSize + 2)) {
                    textFontSize -= 2;
                    chordFontSize -= 2;
                }
                else if((textFontSize >= minimumFontSize + 1) && (chordFontSize >= minimumFontSize + 1)) {
                    textFontSize -= 1;
                    chordFontSize -= 1;
                }
                else if((textFontSize > minimumFontSize) && (chordFontSize > minimumFontSize)) {
                    textFontSize = chordFontSize =minimumFontSize;
                }
            }
        }
        while ((!cancelEvent.isCancelled())&& (width >= screenWidth) && (textFontSize > minimumFontSize) && (chordFontSize > minimumFontSize));
        if(cancelEvent.isCancelled())
            return null;

        int firstMeasureWidth = width;

        do {
            double proposedLargerTextFontSize = textFontSize;
            double proposedLargerChordFontSize = chordFontSize;
            if ((allTextSmallerThanChords) && (textExists) && (textFontSize <= Utils.MAXIMUM_FONT_SIZE - 2) && (proposedLargerTextFontSize<=maximumFontSize-2))
                proposedLargerTextFontSize += 2;
            else if ((allChordsSmallerThanText) && (chordFontSize <= Utils.MAXIMUM_FONT_SIZE - 2)&&(proposedLargerChordFontSize<=maximumFontSize-2))
                proposedLargerChordFontSize += 2;
            else
                // Nothing we can do. Increasing any size will make things bigger than the screen.
                break;
            allTextSmallerThanChords = true;
            allChordsSmallerThanText = true;
            width = 0;
            for (LineSection section : sections) {
                if(cancelEvent.isCancelled())
                    break;
                int textWidth = section.setTextFontSizeAndMeasure(paint, (int) Math.floor(proposedLargerTextFontSize), font, mColorEvent.mLyricColor);
                int chordWidth = section.setChordFontSizeAndMeasure(paint, (int) Math.floor(proposedLargerChordFontSize), font, mColorEvent.mChordColor);
                if (chordWidth > textWidth)
                    allChordsSmallerThanText = false;
                else if ((textWidth > 0) && (textWidth > chordWidth))
                    allTextSmallerThanChords = false;
                width += Math.max(textWidth, chordWidth);
            }
            if(cancelEvent.isCancelled())
                break;
            // If the text still isn't wider than the screen,
            // or it IS wider than the screen, but hasn't got any wider,
            // accept the new sizes.
            if ((width < screenWidth) || (width == firstMeasureWidth)) {
                textFontSize = proposedLargerTextFontSize;
                chordFontSize = proposedLargerChordFontSize;
            }
        }
        while ((!cancelEvent.isCancelled())&& (width < screenWidth) || (width == firstMeasureWidth));
        if(cancelEvent.isCancelled())
            return null;

        mLineTextSize=(int)Math.floor(textFontSize);
        mChordTextSize=(int)Math.floor(chordFontSize);

        mLineDescenderOffset=mChordDescenderOffset=0;
        int actualLineHeight=0;
        int actualLineWidth=0;
        mLyricHeight=mChordHeight=0;
        for (LineSection section : sections)
        {
            if(cancelEvent.isCancelled())
                break;
            section.setTextFontSizeAndMeasure(paint, mLineTextSize, font, mColorEvent.mLyricColor);
            section.setChordFontSizeAndMeasure(paint, mChordTextSize, font, mColorEvent.mChordColor);
            mLineDescenderOffset=Math.max(mLineDescenderOffset,section.mLineSS.mDescenderOffset);
            mChordDescenderOffset=Math.max(mChordDescenderOffset,section.mChordSS.mDescenderOffset);
            mLyricHeight=Math.max(mLyricHeight,section.mTextHeight-section.mLineSS.mDescenderOffset);
            mChordHeight=Math.max(mChordHeight,section.mChordHeight-section.mChordSS.mDescenderOffset);
            actualLineHeight=Math.max(mLyricHeight+mChordHeight+mLineDescenderOffset+mChordDescenderOffset,actualLineHeight);
            actualLineWidth+=Math.max(section.mLineSS.mWidth,section.mChordSS.mWidth);
            highlightColour=section.calculateHighlightedSections(paint,mLineTextSize,font,highlightColour,defaultHighlightColour,errors);
        }
        if(cancelEvent.isCancelled())
            return null;

        // Word wrappin' time!
        if(width>screenWidth)
        {
            LineSection bothersomeSection;
            do {
                // Start from the first section again, but work from off the lefthand edge
                // of the screen if there are already splits.
                // This is because a section could contain one enormous word that spans
                // multiple lines.
                int totalWidth=-getTotalXSplits();
                bothersomeSection=null;
                boolean chordsDrawn=false;
                boolean pixelSplit=false;
                boolean textDrawn=false;
                LineSection firstOnscreenSection=null;
                boolean lastSplitWasPixelSplit=false;
                if(mPixelSplits.size()>0)
                    lastSplitWasPixelSplit=mPixelSplits.get(mPixelSplits.size()-1);
                for(LineSection sec:sections)
                {
                    if(cancelEvent.isCancelled())
                        break;
                    if((totalWidth>0)&&(firstOnscreenSection==null))
                        firstOnscreenSection=sec;
                    int startX=totalWidth;
                    if((startX<=0)&&(startX+sec.mTextWidth>0))
                        textDrawn |= sec.hasText();
                    if((startX<=0)&&(startX+sec.mChordTrimWidth>0)&&(lastSplitWasPixelSplit))
                        chordsDrawn |= sec.hasChord();
                    totalWidth+=sec.getWidth();
                    if((startX>=0)&&(totalWidth<screenWidth))
                    {
                        // this whole section fits onscreen, no problem.
                        chordsDrawn |= sec.hasChord();
                        textDrawn |= sec.hasText();
                    }
                    else if(totalWidth>=screenWidth)
                    {
                        bothersomeSection = sec;
                        break;
                    }
                }
                if(bothersomeSection!=null)
                {
                    int previousSplit=mXSplits.size()>0?mXSplits.get(mXSplits.size()-1):screenWidth;
                    int leftoverSpaceOnPreviousLine=screenWidth-previousSplit;
                    int widthWithoutBothersomeSection = totalWidth - bothersomeSection.getWidth();
                    int xSplit = 0;
                    int lineWidth=0;
                    boolean sectionChordOnscreen=bothersomeSection.hasChord() && ((widthWithoutBothersomeSection>=0) || (widthWithoutBothersomeSection+bothersomeSection.mChordTrimWidth>leftoverSpaceOnPreviousLine));
                    boolean sectionTextOnscreen=bothersomeSection.hasText() && ((widthWithoutBothersomeSection>=0)||(widthWithoutBothersomeSection+bothersomeSection.mTextWidth>0));
                    // Find the last word that fits onscreen.
                    String[] bits = Utils.splitText(bothersomeSection.mLineText);
                    int wordCount=Utils.countWords(bits);
                    boolean splitOnLetter=(wordCount<=1);
                    if (!splitOnLetter) {
                        for (int f = wordCount-1; f>=1 && !cancelEvent.isCancelled(); --f) {
                            String tryThisWithWhitespace = Utils.stitchBits(bits, f);
                            String tryThis = tryThisWithWhitespace.trim();
                            int tryThisWidth = ScreenString.getStringWidth(paint, tryThis, font, mLineTextSize);
                            int tryThisWithWhitespaceWidth=tryThisWidth;
                            if(tryThisWithWhitespace.length()>tryThis.length())
                                tryThisWithWhitespaceWidth = ScreenString.getStringWidth(paint, tryThisWithWhitespace, font, mLineTextSize);
                            if ((tryThisWidth >= bothersomeSection.mChordTrimWidth) || ((tryThisWidth<bothersomeSection.mChordTrimWidth)&&(bothersomeSection.mChordTrimWidth+widthWithoutBothersomeSection<screenWidth)))
                            {
                                int possibleSplitPoint = widthWithoutBothersomeSection + tryThisWidth;
                                int possibleSplitPointWithWhitespace = widthWithoutBothersomeSection + tryThisWithWhitespaceWidth;
                                if (possibleSplitPoint <= 0) {
                                    // We're back where we started. The word we've found must be huge.
                                    // Let's split on letter.
                                    splitOnLetter = true;
                                    break;
                                } else if (possibleSplitPoint < screenWidth) {
                                    // We have a winner!
                                    if(bothersomeSection.mChordDrawLine==-1)
                                        bothersomeSection.mChordDrawLine=mXSplits.size();
                                    chordsDrawn|=sectionChordOnscreen;
                                    textDrawn|=sectionTextOnscreen;
                                    lineWidth=xSplit = possibleSplitPointWithWhitespace;
                                    if((tryThisWidth<bothersomeSection.mChordTrimWidth)&&(bothersomeSection.mChordTrimWidth+widthWithoutBothersomeSection<screenWidth))
                                        lineWidth = bothersomeSection.mChordTrimWidth + widthWithoutBothersomeSection;
                                    break;
                                }
                            } else {
                                // Can we split on the section?
                                if(widthWithoutBothersomeSection>0)
                                {
                                    lineWidth=xSplit = widthWithoutBothersomeSection;
                                }
                                else {
                                    // No? Have to split to pixel
                                    chordsDrawn|=sectionChordOnscreen;
                                    textDrawn|=sectionTextOnscreen;
                                    lineWidth=xSplit = screenWidth;
                                    pixelSplit=true;
                                }
                                break;
                            }
                        }
                        // Not even just the first word fits.
                        if(xSplit==0)
                        {
                            if((firstOnscreenSection==null)||(firstOnscreenSection==bothersomeSection))
                            {
                                // the current section starts before the left margin.
                                // we must be in the middle of a very big word.
                                // OR
                                // This is the first section, and the first word doesn't fit.
                                splitOnLetter=true;
                            }
                            else
                            {
                                // This is the second or subsequent section in this line.
                                // Safe to break on the section.
                                lineWidth=xSplit = widthWithoutBothersomeSection;
                            }
                        }
                    }
                    if(splitOnLetter)
                    {
                        // There is only one word in this section, or there are multiple words
                        // but the first one is enormous.
                        if (firstOnscreenSection==null)
                        {
                            // This section starts BEFORE the left margin, so we have to split on a letter.
                            bits = Utils.splitIntoLetters(bothersomeSection.mLineText);
                            if (bits.length > 1) {
                                for (int f = bits.length-1; f>=1 && !cancelEvent.isCancelled(); --f) {
                                    String tryThis = Utils.stitchBits(bits, f);
                                    int tryThisWidth = ScreenString.getStringWidth(paint, tryThis, font, mLineTextSize);
                                    if (tryThisWidth >= bothersomeSection.mChordTrimWidth)
                                    {
                                        int possibleSplitPoint=widthWithoutBothersomeSection + tryThisWidth;
                                        if(possibleSplitPoint<=0)
                                        {
                                            // We're back where we started. The letter we've found must be huge!
                                            // Just have to split on pixel.
                                            pixelSplit=true;
                                            textDrawn|=sectionTextOnscreen;
                                            chordsDrawn|=sectionChordOnscreen;
                                            lineWidth=xSplit = screenWidth;
                                            break;
                                        }
                                        else if (possibleSplitPoint < screenWidth)
                                        {
                                            // We have a winner!
                                            textDrawn|=sectionTextOnscreen;
                                            chordsDrawn|=sectionChordOnscreen;
                                            if(bothersomeSection.mChordDrawLine==-1)
                                                bothersomeSection.mChordDrawLine=mXSplits.size();
                                            lineWidth=xSplit = possibleSplitPoint;
                                            break;
                                        }
                                    }
                                    else
                                    {
                                        // We're not going to split the chord, and there's only one section,
                                        // so we can't split on that. Just going to have to split to the pixel.
                                        chordsDrawn|=sectionChordOnscreen;
                                        textDrawn|=sectionTextOnscreen;
                                        lineWidth=xSplit = screenWidth;
                                        pixelSplit=true;
                                        break;
                                    }
                                }
                            }
                            else
                            {
                                // There is no text to split. Just going to have to split to the pixel.
                                chordsDrawn|=sectionChordOnscreen;
                                textDrawn|=sectionTextOnscreen;
                                lineWidth=xSplit = screenWidth;
                                pixelSplit=true;
                            }
                        } else {
                            // Split on the section.
                            lineWidth=xSplit = widthWithoutBothersomeSection;
                        }
                    }
                    if (xSplit > 0)
                        mXSplits.add(xSplit);
                    if (lineWidth > 0)
                        mLineWidths.add(lineWidth);
                }
                mTextDrawn.add(textDrawn);
                mPixelSplits.add(pixelSplit);
                mChordsDrawn.add(chordsDrawn);
            }
            while((!cancelEvent.isCancelled()) && (bothersomeSection!=null));
            if(cancelEvent.isCancelled())
                return null;
        }

        int lines=mXSplits.size()+1;
        ArrayList<Integer> graphicHeights=new ArrayList<>();
        if(lines==1)
            graphicHeights.add(actualLineHeight);
        else
        {
            // If we are splitting the line over multiple lines, then the height will increase.
            // If the height is too much for the screen, then we have to X scroll.
            int totalLineHeight=0;
            for(int f=0;f<lines;++f)
            {
                int thisLineHeight=(actualLineHeight - (mChordsDrawn.get(f)?0:(mChordHeight + mChordDescenderOffset)))-(mTextDrawn.get(f)?0:(mLyricHeight + mLineDescenderOffset));
                totalLineHeight+=thisLineHeight;
                graphicHeights.add(thisLineHeight);
            }
            actualLineHeight = totalLineHeight;
            actualLineWidth=calculateWidestLineWidth(actualLineWidth);
        }

        return new LineMeasurements(lines,actualLineWidth,actualLineHeight,graphicHeights,highlightColour,mLineEvent,mNextLine,mYStartScrollTime,scrollMode);
    }

    private int calculateWidestLineWidth(int totalLineWidth)
    {
        int widest=0;
        for(Integer i:mLineWidths)
            widest=Math.max(i,widest);
        totalLineWidth-=getTotalXSplits();
        widest=Math.max(totalLineWidth,widest);
        return widest;
    }

    private int getTotalXSplits()
    {
        int total=0;
        for(Integer i:mXSplits)
            total+=i;
        return total;
    }

    Collection<LineGraphic> getGraphics(boolean allocate)
    {
        for(int f=0;f<mLineMeasurements.mLines;++f)
        {
            LineGraphic graphic=mGraphics.get(f);
            if ((graphic.mLastDrawnLine != this) &&(allocate)) {
                Paint paint = new Paint();
                boolean chordsDrawn=(mChordsDrawn.size()>f?mChordsDrawn.get(f):true);
                int thisLineHeight=mLineMeasurements.mGraphicHeights[f];
                Canvas c = new Canvas(graphic.mBitmap);
                int currentX = 0;
                for(int g=0;(g<mXSplits.size()) && (g<f);++g)
                    currentX-=mXSplits.get(g);
                paint.setTypeface(mFont);
                c.drawColor(0x0000ffff, PorterDuff.Mode.SRC); // Fill with transparency.
                int xSplit=mXSplits.size()>f?mXSplits.get(f):Integer.MAX_VALUE;
                LineSection section = mFirstLineSection;
                while ((section != null) && (currentX<xSplit)) {
                    int width = section.getWidth();
                    if(currentX+width>0) {
                        if ((chordsDrawn)&&((section.mChordDrawLine==f)||(section.mChordDrawLine==-1))&&(currentX<xSplit)&&(section.mChordText.trim().length() > 0))
                        {
                            paint.setColor(section.mIsChord?mColorEvent.mChordColor:mColorEvent.mAnnotationColor);
                            paint.setTextSize(mChordTextSize * Utils.FONT_SCALING);
                            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
                            c.drawText(section.mChordText, currentX, mChordHeight, paint);
                        }
                        c.save();
                        if(xSplit!=Integer.MAX_VALUE)
                            c.clipRect(0,0,xSplit,thisLineHeight);
                        if (section.mLineText.trim().length() > 0) {
                            paint.setColor(mColorEvent.mLyricColor);
                            paint.setTextSize(mLineTextSize*Utils.FONT_SCALING);
                            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
                            c.drawText(section.mLineText, currentX, (chordsDrawn ? mChordHeight + mChordDescenderOffset : 0) + mLyricHeight, paint);
                        }
                        for (ColorRect rect : section.mHighlightingRectangles) {
                            paint.setColor(rect.getColor());
                            c.drawRect(new Rect(rect.getLeft() + currentX, (chordsDrawn?mChordHeight + mChordDescenderOffset:0), rect.getRight() + currentX, (chordsDrawn?mChordHeight +mChordDescenderOffset:0) + mLyricHeight +  mLineDescenderOffset), paint);
                        }
                        c.restore();
                    }
                    section = section.mNextSection;
                    currentX += width;
                }
                graphic.mLastDrawnLine = this;
            }
        }
        return mGraphics;
    }
}
