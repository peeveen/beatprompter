package com.stevenfrew.beatprompter;

import java.util.ArrayList;

public class LineMeasurements
{
    int mLineWidth;
    int mLineHeight;
    long[] mPixelsToTimes;
    int[] mGraphicHeights;
    int[] mJumpScrollIntervals=new int[101];
    int mLines;
    int mHighlightColour;

    LineMeasurements(int lines,int lineWidth,int lineHeight,ArrayList<Integer> graphicHeights,int highlightColour,LineEvent lineEvent,Line nextLine,long yStartScrollTime,ScrollingMode scrollMode)
    {
        mLines=lines;
        mLineHeight=lineHeight;
        mLineWidth=lineWidth;
        mGraphicHeights=new int[graphicHeights.size()];
        for(int f=0;f<mGraphicHeights.length;++f)
            mGraphicHeights[f]=graphicHeights.get(f);
        mHighlightColour=highlightColour;

        for(int f=0;f<101;++f)
        {
            double percentage=((double)f/100.0);
            mJumpScrollIntervals[f]=Math.min((int)((double)lineHeight*Utils.mSineLookup[(int)(90.0*percentage)]),lineHeight);
        }

        mPixelsToTimes=new long[Math.max(1, lineHeight)];
        long lineStartTime=lineEvent.mEventTime;
        long lineEndTime=lineEvent.mEventTime+((scrollMode==ScrollingMode.Smooth || nextLine!=null)?lineEvent.mDuration:0);
        long timeDiff=lineEndTime-yStartScrollTime;
        mPixelsToTimes[0]=lineStartTime;
        for(int f=1;f<lineHeight;++f)
        {
            double linePercentage = ((double) f / (double) lineHeight);
            if(scrollMode==ScrollingMode.Beat) {
                int sineLookup = (int) (90.0 * linePercentage);
                long sineTimeDiff = (long) (timeDiff * Utils.mSineLookup[sineLookup]);
                mPixelsToTimes[f] = yStartScrollTime + sineTimeDiff;
            }
            else
            {
                long pixelTimeDiff=(long)(linePercentage*(double)timeDiff);
                mPixelsToTimes[f]=yStartScrollTime+pixelTimeDiff;
            }
        }
    }
}
