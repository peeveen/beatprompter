package com.stevenfrew.beatprompter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.Collection;

abstract class Line {
    protected Line mPrevLine=null, mNextLine=null;
    int mSongPixelPosition;
    protected ColorEvent mColorEvent; // style event that occurred immediately before this line will be shown.
    LineEvent mLineEvent; // the LineEvent that will display this line.
    protected ArrayList<LineGraphic> mGraphics=new ArrayList<>(); // pointer to the allocated graphic, if one exists
    LineMeasurements mLineMeasurements;
    protected long mYStartScrollTime,mYStopScrollTime;
    ScrollingMode mScrollingMode;

    int mBars=-1; // How many bars does this line last?
    int mScrollbeat=0;
    int mBPB=0;
    int mScrollbeatOffset=0;

    Line(Context context,Collection<Tag> lineTags, int bars, ColorEvent lastColor,int bpb, int scrollbeat,int scrollbeatOffset, ScrollingMode scrollingMode,ArrayList<FileParseError> parseErrors)
    {
        mBPB=bpb;
        mScrollingMode=scrollingMode;
        mScrollbeat=scrollbeat;
        mScrollbeatOffset=scrollbeatOffset;
        for(Tag tag:lineTags)
            if(!tag.mChordTag)
                if((tag.mName.equals("b"))||(tag.mName.equals("bars")))
                    bars=Tag.getIntegerValueFromTag(context,tag,1,128,1,parseErrors);
        mBars=Math.max(1,bars);
        mColorEvent=lastColor;
    }

    int measure(Context context,Paint paint, float minimumFontSize, float maximumFontSize, int screenWidth, int screenHeight, Typeface font, int highlightColour, int defaultHighlightColour, ArrayList<FileParseError> errors, int songPixelPosition, ScrollingMode scrollMode,CancelEvent cancelEvent)
    {
        mSongPixelPosition=songPixelPosition;
        mLineMeasurements=doMeasurements(context,paint,minimumFontSize,maximumFontSize,screenWidth,screenHeight,font,highlightColour,defaultHighlightColour,errors,scrollMode,cancelEvent);
        if(mLineMeasurements!=null)
        {
            return mLineMeasurements.mHighlightColour;
        }
        return 0;
    }

    abstract boolean hasOwnGraphics();

    abstract LineMeasurements doMeasurements(Context context,Paint paint, float minimumFontSize, float maximumFontSize, int screenWidth, int screenHeight, Typeface font, int highlightColour, int defaultHighlightColour, ArrayList<FileParseError> errors, ScrollingMode scrollMode, CancelEvent cancelEvent);

    Line getLastLine()
    {
        Line l=this;
        for(;;)
        {
            if(l.mNextLine==null)
                return l;
            l=l.mNextLine;
        }
    }

    long getTimeFromPixel(int pixelPosition)
    {
        if(pixelPosition==0)
            return 0;
        if((pixelPosition>=mSongPixelPosition)&&(pixelPosition<mSongPixelPosition+mLineMeasurements.mPixelsToTimes.length))
            return mLineMeasurements.mPixelsToTimes[pixelPosition-mSongPixelPosition];
        else if((pixelPosition<mSongPixelPosition)&&(mPrevLine!=null))
            return mPrevLine.getTimeFromPixel(pixelPosition);
        else if((pixelPosition>=mSongPixelPosition+mLineMeasurements.mPixelsToTimes.length)&&(mNextLine!=null))
            return mNextLine.getTimeFromPixel(pixelPosition);
        return mLineMeasurements.mPixelsToTimes[mLineMeasurements.mPixelsToTimes.length-1];
    }

    int getPixelFromTime(long time)
    {
        if(time==0)
            return 0;
        long lineEndTime=Long.MAX_VALUE;
        if(mNextLine!=null)
            lineEndTime=mNextLine.mLineEvent.mEventTime;

        if((time>=mLineEvent.mEventTime)&&(time<lineEndTime))
            return calculatePixelFromTime(time);
        else if((time<mLineEvent.mEventTime)&&(mPrevLine!=null))
            return mPrevLine.getPixelFromTime(time);
        else if((time>=lineEndTime)&&(mNextLine!=null))
            return mNextLine.getPixelFromTime(time);
        return mSongPixelPosition+mLineMeasurements.mPixelsToTimes.length;
    }

    private int calculatePixelFromTime(long time)
    {
        int last=mSongPixelPosition;
        for(long n:mLineMeasurements.mPixelsToTimes)
        {
            if(n>time)
                return last;
            last++;
        }
        return last;
    }

    void setGraphic(LineGraphic graphic)
    {
        mGraphics.add(graphic);
    }

    public Collection<LineGraphic> getGraphics()
    {
        return getGraphics(true);
    }

    abstract Collection<LineGraphic> getGraphics(boolean allocate);

    void insertAfter(Line line)
    {
        line.mNextLine=mNextLine;
        if(mNextLine!=null)
            mNextLine.mPrevLine=line;
        line.mPrevLine = this;
        this.mNextLine=line;
    }

    void recycleGraphics()
    {
        for(LineGraphic g:getGraphics(false))
            if(g!=null)
                g.recycle();
    }
}
