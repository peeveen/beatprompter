package com.stevenfrew.beatprompter;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;

import com.stevenfrew.beatprompter.cache.AudioFile;
import com.stevenfrew.beatprompter.cache.FileParseError;
import com.stevenfrew.beatprompter.cache.SongFile;
import com.stevenfrew.beatprompter.event.BaseEvent;
import com.stevenfrew.beatprompter.event.CancelEvent;
import com.stevenfrew.beatprompter.event.CommentEvent;
import com.stevenfrew.beatprompter.event.LineEvent;
import com.stevenfrew.beatprompter.midi.BeatBlock;
import com.stevenfrew.beatprompter.midi.OutgoingMessage;

import java.util.ArrayList;

public class Song
{
    public SongFile mSongFile;
    ScrollingMode mScrollingMode;
    private Line mFirstLine; // First line to show.
    Line mCurrentLine;
    Line mLastLine;
    PointF mSongTitleHeaderLocation;
    ScreenString mSongTitleHeader;
    BaseEvent mFirstEvent; // First event in the event chain.
    BaseEvent mCurrentEvent; // Last event that executed.
    private BaseEvent mNextEvent; // Upcoming event.
    boolean mCancelled=false;
    private ArrayList<BeatBlock> mBeatBlocks;
    private int mNumberOfMIDIBeatBlocks;

    int mInitialBPB;
    Rect mBeatCounterRect;
    public int mCountIn;
    int mBeatCounterHeight;
    int mSmoothScrollOffset;
    int mSongHeight=0;
    private int mMaxLineHeight=0;
    boolean mStartedByBandLeader;
    ArrayList<OutgoingMessage> mInitialMIDIMessages;
    private ArrayList<Comment> mInitialComments; // Comments to show on startup screen.
    AudioFile mChosenBackingTrack;
    int mChosenBackingTrackVolume;
    private ArrayList<FileParseError> mParseErrors;
    ArrayList<ScreenString> mStartScreenStrings=new ArrayList<>();
    ScreenString mNextSongString=null;
    int mTotalStartScreenTextHeight;
    boolean mSendMidiClock;
    String mNextSong;
    int mOrientation;

    public Song(SongFile songFile, AudioFile audioFile, int audioVolume, ArrayList<Comment> initialComments, BaseEvent firstEvent, Line firstLine, ArrayList<FileParseError> errors, ScrollingMode scrollingMode, boolean sendMidiClock, boolean startedByBandLeader, String nextSong, int orientation, ArrayList<OutgoingMessage> initialMIDIMessages, ArrayList<BeatBlock> beatBlocks, int initialBPB, int countIn)
    {
        mSongFile=songFile;
        mInitialBPB=initialBPB;
        mCountIn=countIn;
        mInitialMIDIMessages=initialMIDIMessages;
        mOrientation=orientation;
        mNextSong=nextSong;
        mStartedByBandLeader=startedByBandLeader;
        mSendMidiClock =sendMidiClock;
        mInitialComments=initialComments;
        mChosenBackingTrack=audioFile;
        mChosenBackingTrackVolume=audioVolume;
        mParseErrors=errors;
        mFirstEvent=mCurrentEvent=firstEvent;
        mBeatBlocks=beatBlocks;
        mNumberOfMIDIBeatBlocks=mBeatBlocks.size();
        mNextEvent=mFirstEvent;
        mCurrentLine=mFirstLine=firstLine;
        mLastLine=mFirstLine;
        if(mLastLine!=null)
            while(mLastLine.mNextLine!=null)
                mLastLine=mLastLine.mNextLine;
        mScrollingMode=scrollingMode;
    }

    void setProgress(long nano)
    {
        BaseEvent e=mCurrentEvent;
        if(e==null)
            e=mFirstEvent;
        if(e!=null)
        {
            BaseEvent newCurrentEvent=e.findEventOnOrBefore(nano);
            mCurrentEvent=newCurrentEvent;
            mNextEvent=mCurrentEvent.mNextEvent;
            LineEvent newCurrentLineEvent=newCurrentEvent.mPrevLineEvent;
            if(newCurrentLineEvent!=null)
                mCurrentLine=newCurrentLineEvent.mLine;
            else
                mCurrentLine=mFirstLine;
        }
    }

    public void doMeasurements(Paint paint, CancelEvent cancelEvent, Handler handler, SongDisplaySettings nativeSettings, SongDisplaySettings sourceSettings)
    {
        Typeface boldFont = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        Typeface notBoldFont = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);

        int sourceScreenWidth=sourceSettings.mScreenWidth;
        int sourceScreenHeight=sourceSettings.mScreenHeight;
        double sourceRatio=(double)sourceScreenWidth/(double)sourceScreenHeight;

        int nativeScreenWidth=nativeSettings.mScreenWidth;
        int nativeScreenHeight=nativeSettings.mScreenHeight;
        boolean screenWillRotate=(nativeSettings.mOrientation!=sourceSettings.mOrientation);
        if(screenWillRotate)
        {
            int temp=nativeScreenHeight;
            //noinspection SuspiciousNameCombination
            nativeScreenHeight=nativeScreenWidth;
            nativeScreenWidth=temp;
        }
        double nativeRatio=(double)nativeScreenWidth/(double)nativeScreenHeight;
        double minRatio=Math.min(nativeRatio,sourceRatio);
        double maxRatio=Math.max(nativeRatio,sourceRatio);
        double ratioMultiplier=minRatio/maxRatio;

        float minimumFontSize=sourceSettings.mMinFontSize;
        float maximumFontSize=sourceSettings.mMaxFontSize;
        minimumFontSize*=ratioMultiplier;
        maximumFontSize*=ratioMultiplier;

        if (minimumFontSize > maximumFontSize) {
            mParseErrors.add(new FileParseError(null,BeatPrompterApplication.getResourceString(R.string.fontSizesAllMessedUp)));
            maximumFontSize = minimumFontSize;
        }

        SharedPreferences sharedPref = BeatPrompterApplication.getPreferences();

        int defaultHighlightColour = Utils.makeHighlightColour(sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_highlightColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_highlightColor_default))));
        boolean showKey = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showSongKey_key), Boolean.parseBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showSongKey_defaultValue)));
        showKey&=((mSongFile.mKey!=null)&&(mSongFile.mKey.length()>0));
        String showBPMString = sharedPref.getString(BeatPrompterApplication.getResourceString(R.string.pref_showSongBPM_key), BeatPrompterApplication.getResourceString(R.string.pref_showSongBPM_defaultValue));

        mBeatCounterHeight=0;
        // Top 5% of screen is used for beat counter
        if (mScrollingMode != ScrollingMode.Manual)
            mBeatCounterHeight = (int) (nativeScreenHeight / 20.0);
        mBeatCounterRect = new Rect(0, 0, nativeScreenWidth, mBeatCounterHeight);

        float maxSongTitleWidth=nativeScreenWidth*0.9f;
        float maxSongTitleHeight=mBeatCounterHeight*0.9f;
        float vMargin=(mBeatCounterHeight-maxSongTitleHeight)/2.0f;
        mSongTitleHeader=ScreenString.create(mSongFile.mTitle,paint,(int)maxSongTitleWidth,(int)maxSongTitleHeight,Utils.makeHighlightColour( Color.BLACK,(byte)0x80),notBoldFont,false);
        float extraMargin=(maxSongTitleHeight-mSongTitleHeader.mHeight)/2.0f;
        float x=(float)((nativeScreenWidth-mSongTitleHeader.mWidth)/2.0);
        float y=mBeatCounterHeight-(extraMargin+mSongTitleHeader.mDescenderOffset+vMargin);
        mSongTitleHeaderLocation=new PointF(x,y);
        Line line=mFirstLine;
        int lineCount=0;
        while(line!=null)
        {
            ++lineCount;
            line=line.mNextLine;
        }
        line=mFirstLine;
        int highlightColour=0;
        mSongHeight=0;
        int lastNonZeroLineHeight=0;
        int lineCounter=0;
        handler.obtainMessage(EventHandler.SONG_LOAD_LINE_PROCESSED,0,lineCount).sendToTarget();
        while((line!=null)&&(!cancelEvent.isCancelled()))
        {
            highlightColour = line.measure(paint, minimumFontSize, maximumFontSize, nativeScreenWidth, nativeScreenHeight,notBoldFont, highlightColour, defaultHighlightColour, mParseErrors, mSongHeight,mScrollingMode,cancelEvent);
            int thisLineHeight=0;
            if(line.mLineMeasurements!=null)
                thisLineHeight=line.mLineMeasurements.mLineHeight;
            if(thisLineHeight>mMaxLineHeight)
                mMaxLineHeight=thisLineHeight;
            if(thisLineHeight>0)
                lastNonZeroLineHeight=thisLineHeight;
            mSongHeight+=thisLineHeight;
            line=line.mNextLine;
            handler.obtainMessage(EventHandler.SONG_LOAD_LINE_PROCESSED,++lineCounter,lineCount).sendToTarget();
        }
        if(cancelEvent.isCancelled())
            return;

        mSmoothScrollOffset = 0;
        if (mScrollingMode == ScrollingMode.Smooth)
            mSmoothScrollOffset = Math.min(mMaxLineHeight, (int) (nativeScreenHeight / 3.0));
        else if(mScrollingMode==ScrollingMode.Beat)
            mSongHeight -= lastNonZeroLineHeight;

        // Measure the popup comments.
        BaseEvent event=mFirstEvent;
        while(event!=null)
        {
            if(event instanceof CommentEvent)
            {
                CommentEvent ce=(CommentEvent)event;
                ce.doMeasurements(nativeScreenWidth,nativeScreenHeight,paint,notBoldFont);
            }
            event=event.mNextEvent;
        }

        // As for the start screen display (title/artist/comments/"press go"),
        // the title should take up no more than 20% of the height, the artist
        // no more than 10%, also 10% for the "press go" message.
        // The rest of the space is allocated for the comments and error messages,
        // each line no more than 10% of the screen height.
        int availableScreenHeight=nativeScreenHeight;
        if((mNextSong!=null)&&(mNextSong.length()>0))
        {
            // OK, we have a next song title to display.
            // This should take up no more than 15% of the screen.
            // But that includes a border, so use 13 percent for the text.
            int eightPercent=(int)(nativeScreenHeight*0.13);
            String fullString=">>> "+mNextSong+" >>>";
            mNextSongString=ScreenString.create(fullString,paint,nativeScreenWidth,eightPercent,Color.BLACK,boldFont,true);
            availableScreenHeight-=nativeScreenHeight*0.15f;
        }
        int tenPercent=(int)(availableScreenHeight/10.0);
        int twentyPercent=(int)(availableScreenHeight/5.0);
        mStartScreenStrings.add(ScreenString.create(mSongFile.mTitle,paint,nativeScreenWidth,twentyPercent, Color.YELLOW,boldFont,true));
        if((mSongFile.mArtist!=null)&&(mSongFile.mArtist.length()>0))
            mStartScreenStrings.add(ScreenString.create(mSongFile.mArtist,paint,nativeScreenWidth,tenPercent, Color.YELLOW,boldFont,true));
        ArrayList<String> commentLines=new ArrayList<>();
        for(Comment c:mInitialComments)
           commentLines.add(c.mText);
        ArrayList<String> nonBlankCommentLines=new ArrayList<>();
        for(String commentLine:commentLines)
           if(commentLine.trim().length()>0)
               nonBlankCommentLines.add(commentLine.trim());
        int errors=mParseErrors.size();
        int messages=Math.min(errors,6)+nonBlankCommentLines.size();
        boolean showBPM=(!BeatPrompterApplication.getResourceString(R.string.showBPMNo).equalsIgnoreCase(showBPMString)) &&(mSongFile.mBPM!=0.0);
        if(showBPM)
            ++messages;
        if(showKey)
            ++messages;
        if(messages>0) {
            int remainingScreenSpace = nativeScreenHeight - (twentyPercent * 2);
            int spacePerMessageLine = (int) Math.floor(remainingScreenSpace / messages);
            spacePerMessageLine = Math.min(spacePerMessageLine, tenPercent);
            int errorCounter = 0;
            for (FileParseError error : mParseErrors) {
                if(cancelEvent.isCancelled())
                    break;
                mStartScreenStrings.add(ScreenString.create(error.getErrorMessage(), paint, nativeScreenWidth, spacePerMessageLine, Color.RED, notBoldFont, false));
                ++errorCounter;
                --errors;
                if ((errorCounter == 5) && (errors > 0)) {
                    mStartScreenStrings.add(ScreenString.create(String.format(BeatPrompterApplication.getResourceString(R.string.otherErrorCount),errors), paint, nativeScreenWidth, spacePerMessageLine, Color.RED, notBoldFont, false));
                    break;
                }
            }
            for (String nonBlankComment : nonBlankCommentLines) {
                if(cancelEvent.isCancelled())
                    break;
                mStartScreenStrings.add(ScreenString.create(nonBlankComment, paint, nativeScreenWidth, spacePerMessageLine, Color.WHITE, notBoldFont, false));
            }
            if(showKey)
            {
                String keyString=BeatPrompterApplication.getResourceString(R.string.keyPrefix)+": "+mSongFile.mKey;
                mStartScreenStrings.add(ScreenString.create(keyString,paint,nativeScreenWidth,spacePerMessageLine,Color.CYAN,notBoldFont,false));
            }
            if(showBPM)
            {
                boolean rounded=BeatPrompterApplication.getResourceString(R.string.showBPMYesRoundedValue).equalsIgnoreCase(showBPMString);
                if(mSongFile.mBPM==(int)mSongFile.mBPM)
                    rounded=true;
                String bpmString=BeatPrompterApplication.getResourceString(R.string.bpmPrefix)+": ";
                if(rounded)
                    bpmString+=(int)Math.round(mSongFile.mBPM);
                else
                    bpmString+=mSongFile.mBPM;
                mStartScreenStrings.add(ScreenString.create(bpmString,paint,nativeScreenWidth,spacePerMessageLine,Color.CYAN,notBoldFont,false));
            }
        }
        if(cancelEvent.isCancelled())
            return;
        if(mScrollingMode!=ScrollingMode.Manual)
            mStartScreenStrings.add(ScreenString.create(BeatPrompterApplication.getResourceString(R.string.tapTwiceToStart),paint,nativeScreenWidth,tenPercent,Color.GREEN,boldFont,true));
        mTotalStartScreenTextHeight=0;
        for(ScreenString ss: mStartScreenStrings)
            mTotalStartScreenTextHeight+=ss.mHeight;

/*        if((mScrollingMode==ScrollingMode.Smooth)&&(mFirstLine!=null))
        {
            // Prevent Y scroll of all final lines that fit onscreen.
            int totalHeight=0;
            Line lastLine=mFirstLine.getLastLine();
            boolean onLastLine=true;
            while(lastLine!=null)
            {
                if(totalHeight+lastLine.mActualLineHeight>availableScreenHeight)
                    break;
                totalHeight+=lastLine.mActualLineHeight;
                lastLine.mLineEvent.remove();
                lastLine.mYStartScrollTime=lastLine.mYStopScrollTime=Long.MAX_VALUE;
                if(!onLastLine)
                    mSongHeight-=lastLine.mActualLineHeight;
                lastLine=lastLine.mPrevLine;
                onLastLine=false;
            }
            // BUT! Add height of tallest line to compensate for scrollmode line offset
            mSongHeight+=mMaxLineHeight;
        }*/

        // Allocate graphics objects.
        int maxGraphicsRequired = getMaximumGraphicsRequired(nativeScreenHeight);
        LineGraphic[] mLineGraphics = new LineGraphic[maxGraphicsRequired];
        for (int f = 0; f < maxGraphicsRequired; ++f)
        {
            Rect maxLineSize = getBiggestLineSize(f,maxGraphicsRequired);
            mLineGraphics[f] = new LineGraphic(maxLineSize);
            if (f > 0) {
                mLineGraphics[f].mPrevGraphic = mLineGraphics[f - 1];
                mLineGraphics[f - 1].mNextGraphic = mLineGraphics[f];
            }
        }
        if(mLineGraphics.length>0) {
            mLineGraphics[0].mPrevGraphic = mLineGraphics[mLineGraphics.length - 1];
            mLineGraphics[mLineGraphics.length - 1].mNextGraphic = mLineGraphics[0];
        }

        line = mFirstLine;
        if(line!=null) {
            LineGraphic graphic = mLineGraphics[0];
            while (line != null) {
//                if(!line.hasOwnGraphics())
                    for (int f = 0; f < line.mLineMeasurements.mLines; ++f) {
                        line.setGraphic(graphic);
                        graphic = graphic.mNextGraphic;
                    }
                line = line.mNextLine;
            }
        }

        // In smooth scrolling mode, the last screenful of text should never leave the screen.
        if(mScrollingMode==ScrollingMode.Smooth)
        {
            int total=(nativeScreenHeight-mSmoothScrollOffset)-mBeatCounterHeight;
            Line prevLastLine=null;
            line=mLastLine;

            while(line!=null)
            {
                total-=line.mLineMeasurements.mLineHeight;
                if(total<=0)
                {
                    if(prevLastLine!=null)
                        prevLastLine.mYStopScrollTime=Long.MAX_VALUE;
                    break;
                }
                line.mLineEvent.remove();
                line.mLineEvent=null;
//                line.mLineEvent.mEventTime=Long.MAX_VALUE;
                prevLastLine=line;
                line=line.mPrevLine;
            }
        }
    }

    BaseEvent getNextEvent(long time)
    {
        if((mNextEvent!=null)&&(mNextEvent.mEventTime<=time))
        {
            mCurrentEvent=mNextEvent;
            mNextEvent=mNextEvent.mNextEvent;
            return mCurrentEvent;
        }
        return null;
    }

    private Rect getBiggestLineSize(int index,int modulus)
    {
        Line line=mFirstLine;
        int maxHeight=0;
        int maxWidth=0;
        int lineCount=0;
        while(line!=null)
        {
            //if(!line.hasOwnGraphics())
            {
                for (int lh : line.mLineMeasurements.mGraphicHeights)
                {
                    if((lineCount%modulus)==index) {
                        maxHeight = Math.max(maxHeight, lh);
                        maxWidth = Math.max(maxWidth, line.mLineMeasurements.mLineWidth);
                    }
                    ++lineCount;
                }
            }
            line = line.mNextLine;
        }
        return new Rect(0,0,maxWidth-1,maxHeight-1);
    }

    private int getMaximumGraphicsRequired(int screenHeight)
    {
        Line line=mFirstLine;
        int maxLines=0;
        while(line!=null)
        {
            Line thisLine=line;
            int heightCounter=0;
            int lineCounter=0;
            while((thisLine!=null)&&(heightCounter<screenHeight))
            {
//                if(!thisLine.hasOwnGraphics())
                {
                    // Assume height of first line to be 1 pixel
                    // This is the state of affairs when the top line is almost
                    // scrolled offscreen, but not quite.
                    int lineHeight = 1;
                    if (lineCounter > 0)
                        lineHeight = thisLine.mLineMeasurements.mLineHeight;
                    heightCounter += lineHeight;
                    lineCounter += thisLine.mLineMeasurements.mLines;
                }
                thisLine = thisLine.mNextLine;
            }

            maxLines = Math.max(maxLines, lineCounter);
            line=line.mNextLine;
        }
        return maxLines;
    }

    long getTimeFromPixel(int pixel)
    {
        if(pixel==0)
            return 0;
        if(mCurrentLine!=null)
            return mCurrentLine.getTimeFromPixel(pixel);
        return mFirstLine.getTimeFromPixel(pixel);
    }

    int getPixelFromTime(long time)
    {
        if(time==0)
            return 0;
        if(mCurrentLine!=null)
            return mCurrentLine.getPixelFromTime(time);
        return mFirstLine.getPixelFromTime(time);
    }

    void recycleGraphics()
    {
        Line line=mFirstLine;
        while(line!=null) {
            line.recycleGraphics();
            line = line.mNextLine;
        }
    }

    long getMIDIBeatTime(int beat)
    {
        for (int f = 0; f < mNumberOfMIDIBeatBlocks; ++f) {
            BeatBlock beatBlock=mBeatBlocks.get(f);
            if ((beatBlock.getMidiBeatCount() <= beat) && ((f + 1 == mNumberOfMIDIBeatBlocks) || (mBeatBlocks.get(f + 1).getMidiBeatCount() > beat))) {
                return (long)(beatBlock.getBlockStartTime()+(beatBlock.getNanoPerBeat()*(beat-beatBlock.getMidiBeatCount())));
            }
        }
        return 0;
    }
}
