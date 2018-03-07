package com.stevenfrew.beatprompter;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

class LineSection
{
    LineSection mNextSection;
    LineSection mPrevSection;
    String mLineText;
    String mChordText;
    boolean mIsChord;
    private Collection<Tag> mTags;
    int mTextWidth=0;
    int mChordWidth=0;
    int mChordTrimWidth=0;
    int mTextHeight=0;
    int mChordHeight=0;
    int mChordDrawLine=-1;
    ScreenString mLineSS;
    ScreenString mChordSS;
    private int mSectionPosition=0;
    ArrayList<ColorRect> mHighlightingRectangles=new ArrayList<>(); // Start/stop/start/stop x-coordinates of highlighted sections.

    LineSection(String lineText,String chordText,int sectionPosition,Collection<Tag> tags)
    {
        mSectionPosition=sectionPosition;
        mLineText=lineText;
        mChordText=chordText;
        mIsChord=Utils.isChord(mChordText);
        mTags=tags;
    }

    int setTextFontSizeAndMeasure(Paint paint,int fontSize,Typeface face,boolean bold,int color)
    {
        mLineSS=ScreenString.create(mLineText,paint,fontSize,face,bold,color);
        if(mLineText.trim().length()==0)
            mTextHeight=0;
        else
            mTextHeight = mLineSS.mHeight;
        return mTextWidth=mLineSS.mWidth;
    }

    int setChordFontSizeAndMeasure(Paint paint,int fontSize,Typeface face,boolean bold,int color)
    {
        mChordSS = ScreenString.create(mChordText, paint, fontSize, face, bold,color);
        String trimChord=mChordText.trim();
        ScreenString trimChordSS;
        if(trimChord.length()<mChordText.length())
            trimChordSS= ScreenString.create(trimChord, paint, fontSize, face, bold,color);
        else
            trimChordSS=mChordSS;
        if(mChordText.trim().length()==0)
            mChordHeight=0;
        else
            mChordHeight = mChordSS.mHeight;
        mChordTrimWidth=trimChordSS.mWidth;
        return mChordWidth=mChordSS.mWidth;
    }

    int getWidth()
    {
        return Math.max(mTextWidth,mChordWidth);
    }
    int getHeight()
    {
        return mTextHeight+mChordHeight;
    }

    int calculateHighlightedSections(Paint paint,float textSize,Typeface face,int currentHighlightColour,int defaultHighlightColour,ArrayList<FileParseError> errors)
    {
        boolean lookingForEnd=(currentHighlightColour!=0);
        int highlightColour=(lookingForEnd?currentHighlightColour:0);
        int startX=0;
        int startPosition=0;
        for(Tag tag : mTags) {
            if ((tag.mName.equals("soh")) && (!lookingForEnd))
            {
                String strHighlightText=mLineText.substring(0,tag.mPosition-mSectionPosition);
                startX=ScreenString.getStringWidth(paint,strHighlightText,face,false,textSize);
                startPosition=tag.mPosition-mSectionPosition;
                if(tag.mValue.length()>0)
                {
                    try
                    {
                        highlightColour=Color.parseColor(tag.mValue);
                    }
                    catch(IllegalArgumentException e)
                    {
                        // We're past the titlescreen at this point, so don't bother.
                        errors.add(new FileParseError(tag,"Could not interpret \""+tag.mValue+"\" as a valid colour. Using default."));
                        highlightColour=defaultHighlightColour;
                    }
                    highlightColour=Utils.makeHighlightColour(highlightColour);
                }
                else
                    highlightColour=defaultHighlightColour;
                lookingForEnd=true;
            }
            else if ((tag.mName.equals("eoh")) && (lookingForEnd))
            {
                String strHighlightText=mLineText.substring(startPosition,tag.mPosition-mSectionPosition);
                int sectionWidth=ScreenString.getStringWidth(paint,strHighlightText,face,false,textSize);
                mHighlightingRectangles.add(new ColorRect(startX,mChordHeight,startX+sectionWidth,mChordHeight+mTextHeight,highlightColour));
                highlightColour=0;
                lookingForEnd=false;
            }
        }
        if(lookingForEnd)
            mHighlightingRectangles.add(new ColorRect(startX,mChordHeight,Math.max(mTextWidth,mChordWidth),mChordHeight+mTextHeight,highlightColour));
        return highlightColour;
    }

    boolean hasChord()
    {
        return (mChordText!=null && mChordText.trim().length()>0);
    }
    boolean hasText()
    {
        return (mLineText!=null && mLineText.trim().length()>0);
    }

}
