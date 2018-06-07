package com.stevenfrew.beatprompter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.OverScroller;
import android.widget.Toast;

import com.stevenfrew.beatprompter.bluetooth.PauseOnScrollStartMessage;
import com.stevenfrew.beatprompter.bluetooth.QuitSongMessage;
import com.stevenfrew.beatprompter.bluetooth.SetSongTimeMessage;
import com.stevenfrew.beatprompter.bluetooth.ToggleStartStopMessage;
import com.stevenfrew.beatprompter.event.BaseEvent;
import com.stevenfrew.beatprompter.event.BeatEvent;
import com.stevenfrew.beatprompter.event.ColorEvent;
import com.stevenfrew.beatprompter.event.CommentEvent;
import com.stevenfrew.beatprompter.event.EndEvent;
import com.stevenfrew.beatprompter.event.LineEvent;
import com.stevenfrew.beatprompter.event.MIDIEvent;
import com.stevenfrew.beatprompter.event.PauseEvent;
import com.stevenfrew.beatprompter.event.TrackEvent;
import com.stevenfrew.beatprompter.midi.MIDITriggerSafetyCatch;

import java.io.FileInputStream;
import java.util.Collection;

public class SongView extends AppCompatImageView implements GestureDetector.OnGestureListener
{

    enum ScreenAction {
        Scroll, Volume, None
    }

    public static int POPUP_MARGIN = 25;
    private static int SONG_END_PEDAL_PRESSES=3;
    private static long SHOW_TEMP_MESSAGE_THRESHOLD_NANOSECONDS = Utils.milliToNano(2000);
    private static int[] mAccelerations = new int[2048];

    static {
        for (int f = 0; f < 2048; ++f)
            mAccelerations[f] = (int) Math.ceil(Math.sqrt(f + 1) * 2.0);
    }

    Rect mBeatCountRect=new Rect();
    private int mEndSongByPedalCounter=0;
    private long mMetronomeBeats=0;
    private boolean mInitialized=false;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mPageUpDownScreenHeight;
    private int mSongScrollEndPixel;
    private boolean mSkipping=false;
    private int mCurrentVolume = 80;
    private CommentEvent mLastCommentEvent = null;
    private long mLastCommentTime = 0;
    private long mLastTempMessageTime = 0;
    private long mLastBeatTime;
    private Paint mPaint;           // The paint (e.g. style, color) used for drawing
    private OverScroller mScroller;
    MetronomeTask mMetronomeTask=null;
    Thread mMetronomeThread=null;

    public Song mSong;

    public int mPageDownPixel = 0;
    public int mPageUpPixel = 0;
    public int mLineDownPixel = 0;
    public int mLineUpPixel = 0;

    private long mSongStartTime;
    private int mStartState = 0;
    private boolean mUserHasScrolled = false;
    private long mPauseTime = 0;
    private long mNanosecondsPerBeat = Utils.nanosecondsPerBeat(120);
    private int[] mBackgroundColorLookup = new int[101];
    private int mCommentTextColor;
    private int mDefaultCurrentLineHighlightColour;
    private int mSongPixelPosition = 0;
    private int mTargetPixelPosition = -1;
    private int mTargetAcceleration = 1;
    private boolean mShowScrollIndicator = true;
    private boolean mShowSongTitle = false;
    private boolean mHighlightCurrentLine = false;
    private int mSongTitleContrastBackground;
    private int mSongTitleContrastBeatCounter;
    private Rect mScrollIndicatorRect;
    private ColorEvent mLastProcessedColorEvent = null;
    private GestureDetectorCompat mGestureDetector;
    private ScreenAction mScreenAction = ScreenAction.Scroll;
    private int mBeatCounterColor = Color.WHITE;
    private int mScrollMarkerColor = Color.BLACK;
    boolean mPulse = true;
    private MediaPlayer mTrackMediaPlayer;
    private MediaPlayer mSilenceMediaPlayer;
    private SoundPool mClickSoundPool;
    private int mClickAudioID;
    private long mCommentDisplayTimeNanoseconds = Utils.milliToNano(4000);
    private BeatPrompterApplication mApp = null;
    private SongDisplayActivity mSongDisplayActivity;
    private Handler mSongDisplayActivityHandler;
    MIDITriggerSafetyCatch mMIDITriggerSafetyCatch;
    boolean mSendMidiClock=false;

    public SongView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScroller = new OverScroller(context);
        mGestureDetector = new GestureDetectorCompat(context, this);
        initView();
    }

    // Constructor
    public SongView(Context context) {
        super(context);
        mScroller = new OverScroller(context);
        mGestureDetector = new GestureDetectorCompat(context, this);
        initView();
    }

    public void init(SongDisplayActivity songDisplayActivity, Handler songDisplayActivityHandler, BeatPrompterApplication app) {
        mSongDisplayActivity = songDisplayActivity;
        mSongDisplayActivityHandler=songDisplayActivityHandler;
        mApp = app;
        mSong = BeatPrompterApplication.getCurrentSong();
        calculateScrollEnd();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(songDisplayActivity);
        mMIDITriggerSafetyCatch=MIDITriggerSafetyCatch.valueOf(sharedPref.getString(songDisplayActivity.getString(R.string.pref_midiTriggerSafetyCatch_key),songDisplayActivity.getString(R.string.pref_midiTriggerSafetyCatch_defaultValue)));
        String metronomePref=sharedPref.getString(songDisplayActivity.getString(R.string.pref_metronome_key), songDisplayActivity.getString(R.string.pref_metronome_defaultValue));

        if(mSong.mInitialBPM!=0) {
            boolean metronomeOn = metronomePref.equals(songDisplayActivity.getString(R.string.metronomeOnValue));
            boolean metronomeOnWhenNoBackingTrack = metronomePref.equals(songDisplayActivity.getString(R.string.metronomeOnWhenNoBackingTrackValue));
            boolean metronomeCount = metronomePref.equals(songDisplayActivity.getString(R.string.metronomeDuringCountValue));

            if(metronomeOnWhenNoBackingTrack && mSong.mChosenBackingTrack == null)
                metronomeOn=true;

            if (metronomeOn)
                mMetronomeBeats = Long.MAX_VALUE;
            else if (metronomeCount)
                mMetronomeBeats = mSong.mCountIn * mSong.mInitialBPB;
        }

        if(mSong!=null) {
            mSendMidiClock = mSong.mSendMidiClock || sharedPref.getBoolean(songDisplayActivity.getString(R.string.pref_sendMidi_key), false);
            mBeatCountRect = new Rect(0, 0, mSong.mBeatCounterRect.width(), mSong.mBeatCounterHeight);
            mHighlightCurrentLine = (mSong.mScrollingMode == ScrollingMode.Beat) && sharedPref.getBoolean(songDisplayActivity.getString(R.string.pref_highlightCurrentLine_key), Boolean.parseBoolean(songDisplayActivity.getString(R.string.pref_highlightCurrentLine_defaultValue)));
        }
    }

    private void calculateScrollEnd()
    {
        int songDisplayEndPixel=mSong.mSongHeight;
        if(mSong.mScrollingMode!=ScrollingMode.Beat)
            songDisplayEndPixel-=mScreenHeight;
        mSongScrollEndPixel=Math.max(0,songDisplayEndPixel);
        if(mSong.mScrollingMode==ScrollingMode.Smooth)
        {
            mSongScrollEndPixel+=mSong.mSmoothScrollOffset;
            mSongScrollEndPixel+=mSong.mBeatCounterHeight;
        }
    }

    private void initView() {
        mClickSoundPool = new SoundPool(16, AudioManager.STREAM_MUSIC, 0);
        mClickAudioID = mClickSoundPool.load(this.getContext(), R.raw.click, 0);
        mPaint = new Paint();
        mSongPixelPosition = 0;

        Context context = this.getContext();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String screenAction = sharedPref.getString(context.getString(R.string.pref_screenAction_key), context.getString(R.string.pref_screenAction_defaultValue));
        if (screenAction.equalsIgnoreCase(context.getString(R.string.screenActionNoneValue)))
            mScreenAction = ScreenAction.None;
        if (screenAction.equalsIgnoreCase(context.getString(R.string.screenActionVolumeValue)))
            mScreenAction = ScreenAction.Volume;
        if (screenAction.equalsIgnoreCase(context.getString(R.string.screenActionScrollPauseAndRestartValue)))
            mScreenAction = ScreenAction.Scroll;
        mShowScrollIndicator = sharedPref.getBoolean(context.getString(R.string.pref_showScrollIndicator_key), Boolean.parseBoolean(context.getString(R.string.pref_showScrollIndicator_defaultValue)));
        mShowSongTitle = sharedPref.getBoolean(context.getString(R.string.pref_showSongTitle_key), Boolean.parseBoolean(context.getString(R.string.pref_showSongTitle_defaultValue)));
        int commentDisplayTimeSeconds = sharedPref.getInt(context.getString(R.string.pref_commentDisplayTime_key), Integer.parseInt(context.getString(R.string.pref_commentDisplayTime_default)));
        commentDisplayTimeSeconds += Integer.parseInt(context.getString(R.string.pref_commentDisplayTime_offset));
        mCommentDisplayTimeNanoseconds = Utils.milliToNano(commentDisplayTimeSeconds * 1000);

        mCommentTextColor = Utils.makeHighlightColour(sharedPref.getInt(context.getString(R.string.pref_commentTextColor_key), Color.parseColor(context.getString(R.string.pref_commentTextColor_default))));
        mDefaultCurrentLineHighlightColour = Utils.makeHighlightColour(sharedPref.getInt(context.getString(R.string.pref_currentLineHighlightColor_key), Color.parseColor(context.getString(R.string.pref_currentLineHighlightColor_default))));
        mPulse = sharedPref.getBoolean(context.getString(R.string.pref_pulse_key), Boolean.parseBoolean(context.getString(R.string.pref_pulse_defaultValue)));
    }

    private void ensureInitialised()
    {
        if(mSong==null)
            return;
        if(!mInitialized)
        {
            if(mSong.mScrollingMode==ScrollingMode.Smooth)
                mPulse=false;
            mInitialized=true;
            // First event will ALWAYS be a style event.
            processColorEvent((ColorEvent) mSong.mFirstEvent);

            if (mSong.mChosenBackingTrack != null)
                if (mSong.mChosenBackingTrack.mFile.exists()) {
                    // Shitty Archos workaround.
                    mTrackMediaPlayer = new MediaPlayer();
                    // Play silence to kickstart audio system, allowing snappier playback.
                    mSilenceMediaPlayer=MediaPlayer.create(getContext(),R.raw.silence);
                    mSilenceMediaPlayer.setLooping(true);
                    mSilenceMediaPlayer.setVolume(0.01f,0.01f);
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(mSong.mChosenBackingTrack.mFile.getAbsolutePath());
                        mTrackMediaPlayer.setDataSource(fis.getFD());
                        mTrackMediaPlayer.prepare();
                        seekTrack(0);
                        mCurrentVolume = mSong.mChosenBackingTrackVolume;
                        mTrackMediaPlayer.setVolume(0.01f * mCurrentVolume, 0.01f * mCurrentVolume);
                        mTrackMediaPlayer.setLooping(false);
                        mSilenceMediaPlayer.start();
                    } catch (Exception e) {
                        mTrackMediaPlayer=null;
                        Toast toast = Toast.makeText(getContext(), R.string.crap_audio_file_warning, Toast.LENGTH_LONG);
                        toast.show();
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (Exception ee) {
                            Log.e(BeatPrompterApplication.TAG,"Failed to close audio file input stream.",ee);
                        }
                    }
                } else {
                    Toast toast = Toast.makeText(getContext(), R.string.missing_audio_file_warning, Toast.LENGTH_LONG);
                    mTrackMediaPlayer=null;
                    mSilenceMediaPlayer=null;
                    toast.show();
                }
                if(mSong.mScrollingMode==ScrollingMode.Manual)
                {
                    if (mMetronomeBeats > 0) {
                        mMetronomeThread = new Thread(mMetronomeTask = new MetronomeTask(mSong.mInitialBPM, mMetronomeBeats));
                        // Infinite metronome? Might as well start it now.
                        if(mMetronomeBeats==Long.MAX_VALUE)
                            mMetronomeThread.start();
                    }
                }

        }
    }

    // Called back to draw the view. Also called by invalidate().
    @Override
    protected void onDraw(Canvas canvas) {
        if (mSong == null)
            return;
        ensureInitialised();
        boolean scrolling = false;
        if (mStartState > 0)
            scrolling = calculateScrolling();
        long timePassed = 0;
        double beatPercent = 1.0;
        boolean showTempMessage = false;
        boolean showComment = false;
        if ((mStartState == 2) && (!scrolling)) {
            long time = System.nanoTime();
            timePassed = Math.max(0, time - mSongStartTime);
            if (mLastBeatTime > 0) {
                long beatTimePassed = Math.max(0, time - mLastBeatTime);
                double beatTime = beatTimePassed % mNanosecondsPerBeat;
                beatPercent = beatTime / mNanosecondsPerBeat;
            }
            if (mSong.mScrollingMode != ScrollingMode.Manual) {
                BaseEvent event;
                while ((event = mSong.getNextEvent(timePassed)) != null) {
                    if (event instanceof ColorEvent)
                        processColorEvent((ColorEvent) event);
                    else if (event instanceof CommentEvent)
                        processCommentEvent((CommentEvent) event, time);
                    else if (event instanceof BeatEvent)
                        processBeatEvent((BeatEvent) event,true);
                    else if (event instanceof MIDIEvent)
                        processMIDIEvent((MIDIEvent) event);
                    else if (event instanceof PauseEvent)
                        processPauseEvent((PauseEvent) event);
                    else if (event instanceof LineEvent)
                        processLineEvent((LineEvent) event);
                    else if (event instanceof TrackEvent)
                        processTrackEvent();
                    else if (event instanceof EndEvent) {
                        if(processEndEvent())
                            return;
                    }
                }
            }
            showTempMessage = (time - mLastTempMessageTime < SHOW_TEMP_MESSAGE_THRESHOLD_NANOSECONDS);
            if (mLastCommentEvent != null)
                if (time - mLastCommentTime < mCommentDisplayTimeNanoseconds)
                    showComment = true;
        }
        int currentY = mSong.mBeatCounterHeight;
        Line currentLine = mSong.mCurrentLine;
        int yScrollOffset = 0;
        int color = mBackgroundColorLookup[(int) (beatPercent * 100.0)];
        canvas.drawColor(color, PorterDuff.Mode.SRC);
        if (currentLine != null) {
            double scrollPercentage = 0.0;
            // If a scroll event in underway, move currentY up
            if ((mStartState < 2) || (mSong.mScrollingMode == ScrollingMode.Manual)) {
                yScrollOffset = mSongPixelPosition - currentLine.mSongPixelPosition;
                if (mSong.mScrollingMode == ScrollingMode.Smooth)
                    scrollPercentage = (double) yScrollOffset / (double) currentLine.mLineMeasurements.mLineHeight;
            } else {
                if ((!scrolling) && (mSong.mScrollingMode != ScrollingMode.Manual)) {
                    if ((currentLine.mYStopScrollTime > timePassed) && (currentLine.mYStartScrollTime <= timePassed))
                        scrollPercentage = (double) (timePassed - currentLine.mYStartScrollTime) / (double) (currentLine.mYStopScrollTime - currentLine.mYStartScrollTime);
                    else if (currentLine.mYStopScrollTime <= timePassed)
                        scrollPercentage = 1.0;
                    if (mSong.mScrollingMode == ScrollingMode.Smooth)
                        yScrollOffset = (int) (currentLine.mLineMeasurements.mLineHeight * scrollPercentage);
                    else if (mSong.mScrollingMode == ScrollingMode.Beat)
                        yScrollOffset = currentLine.mLineMeasurements.mJumpScrollIntervals[(int) (scrollPercentage * 100.0)];
                }
            }
            currentY -= yScrollOffset;
            if (mStartState == 2)
                mSongPixelPosition = currentLine.mSongPixelPosition + yScrollOffset;
            if (mSong.mScrollingMode == ScrollingMode.Smooth)
                currentY += mSong.mSmoothScrollOffset;

            int startY = currentY;
            Line firstLineOnscreen = null;
            boolean startOnscreen = false;
            boolean endOnscreen = false;
            boolean highlight = mHighlightCurrentLine;
            for (; (currentLine != null) && (currentY < mScreenHeight); ) {
                if (currentY > mSong.mBeatCounterHeight - currentLine.mLineMeasurements.mLineHeight) {
                    if (firstLineOnscreen == null) {
                        firstLineOnscreen = currentLine;
                        startOnscreen = currentY >= mSong.mBeatCounterHeight;
                        endOnscreen = currentY + currentLine.mLineMeasurements.mLineHeight <= mScreenHeight;
                    }
                    Collection<LineGraphic> graphics = currentLine.getGraphics();
                    int lineCounter = 0;
                    int lineTop = currentY;
                    for (LineGraphic graphic : graphics) {
                        canvas.drawBitmap(graphic.mBitmap, 0, currentY, mPaint);
                        currentY += currentLine.mLineMeasurements.mGraphicHeights[lineCounter++];
                    }
                    if (highlight) {
                        mPaint.setColor(mDefaultCurrentLineHighlightColour);
                        canvas.drawRect(0, lineTop, mScreenWidth, lineTop + currentLine.mLineMeasurements.mLineHeight, mPaint);
                        mPaint.setAlpha(255);
                    }
                } else
                    currentY += currentLine.mLineMeasurements.mLineHeight;
                currentLine = currentLine.mNextLine;
                highlight = false;
            }
            // Calculate pageup/pagedown/lineup/linedown lines
            if (mSong.mScrollingMode == ScrollingMode.Manual)
                calculateManualScrollingPositions(firstLineOnscreen, currentLine, currentY, startOnscreen, endOnscreen);

            if (mSong.mScrollingMode == ScrollingMode.Smooth) {
                // If we've drawn the end of the last line, stop smooth scrolling.
                Line prevLine = mSong.mCurrentLine.mPrevLine;
                if ((prevLine != null) && (startY > 0)) {
                    mPaint.setAlpha((int) (255.0 - (255.0 * scrollPercentage)));
                    currentY = startY - prevLine.mLineMeasurements.mLineHeight;
                    Collection<LineGraphic> graphics = prevLine.getGraphics();
                    int lineCounter = 0;
                    for (LineGraphic graphic : graphics) {
                        canvas.drawBitmap(graphic.mBitmap, 0, currentY, mPaint);
                        currentY += prevLine.mLineMeasurements.mGraphicHeights[lineCounter++];
                    }
                    mPaint.setAlpha(255);
                }
            }
        }
        mPaint.setColor(mBackgroundColorLookup[100]);
        canvas.drawRect(0, 0, mScreenWidth, mSong.mBeatCounterHeight, mPaint);
        mPaint.setColor(mScrollMarkerColor);
        if ((mSong.mScrollingMode == ScrollingMode.Beat) && (mShowScrollIndicator) && (mScrollIndicatorRect != null))
            canvas.drawRect(mScrollIndicatorRect, mPaint);
        mPaint.setColor(mBeatCounterColor);
        canvas.drawRect(mBeatCountRect, mPaint);
        canvas.drawLine(0, mSong.mBeatCounterHeight, mScreenWidth, mSong.mBeatCounterHeight, mPaint);
        if (mShowSongTitle)
            showSongTitle(canvas);
        if (showTempMessage) {
            if (mEndSongByPedalCounter == 0)
                showTempMessage(mCurrentVolume + "%", 80, Color.BLACK, canvas);
            else {
                String message = "Press pedal " + (SONG_END_PEDAL_PRESSES - mEndSongByPedalCounter) + " more times to end song.";
                showTempMessage(message, 20, Color.BLUE, canvas);
            }
        } else
            mEndSongByPedalCounter = 0;
        if (showComment)
            showComment(canvas);
        if (mStartState > 0)
            invalidate();  // Force a re-draw
        else if ((mStartState == 0) && (mSong != null))
            drawTitleScreen(canvas);
    }

    boolean calculateScrolling()
    {
        boolean scrolling=false;
        if (((mScreenAction == ScreenAction.Scroll) || (mSong.mScrollingMode == ScrollingMode.Manual)) && (mScroller.computeScrollOffset())) {
            mSongPixelPosition = mScroller.getCurrY();
            //if (mSong.mScrollingMode != ScrollingMode.Manual)
            {
                long songTime = mSong.mCurrentLine.getTimeFromPixel(mSongPixelPosition);
                setSongTime(songTime, mStartState == 1, true,false);
            }
            scrolling = true;
        } else if ((mTargetPixelPosition != -1) && (mTargetPixelPosition != mSongPixelPosition)) {
            int diff = Math.min(2048, Math.max(-2048, mTargetPixelPosition - mSongPixelPosition));
            int absDiff = Math.abs(diff);
            int targetAcceleration = Math.min(mAccelerations[absDiff - 1], absDiff);
            if (mTargetAcceleration * 2 < targetAcceleration)
                mTargetAcceleration *= 2;
            else
                mTargetAcceleration = targetAcceleration;
            if (diff > 0)
                mSongPixelPosition += mTargetAcceleration;
            else
                mSongPixelPosition -= mTargetAcceleration;
            if (mSongPixelPosition == mTargetPixelPosition)
                clearScrollTarget();
            long songTime = mSong.mCurrentLine.getTimeFromPixel(mSongPixelPosition);
            setSongTime(songTime, mStartState == 1, true,false);
        }
        return scrolling;
    }

    void drawTitleScreen(Canvas canvas)
    {
        canvas.drawColor(Color.BLACK);
        int midX = mScreenWidth >> 1;
        float fifteenPercent=mScreenHeight*0.15f;
        int startY = (int) Math.floor((mScreenHeight - mSong.mTotalStartScreenTextHeight) / 2);
        ScreenString nextSongSS=mSong.mNextSongString;
        if(nextSongSS!=null) {
            mPaint.setColor(mSkipping?Color.RED:Color.WHITE);
            float halfDiff=(fifteenPercent-nextSongSS.mHeight)/2.0f;
            canvas.drawRect(0,mScreenHeight-fifteenPercent,mScreenWidth,mScreenHeight,mPaint);
            int nextSongY=mScreenHeight-(int)(nextSongSS.mDescenderOffset+halfDiff);
            startY -= fifteenPercent / 2.0f;
            mPaint.setColor(nextSongSS.mColor);
            mPaint.setTextSize(nextSongSS.mFontSize * Utils.FONT_SCALING);
            mPaint.setTypeface(nextSongSS.mFace);
            mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            canvas.drawText(nextSongSS.mText, midX - (nextSongSS.mWidth >> 1), nextSongY, mPaint);
        }
        for (ScreenString ss : mSong.mStartScreenStrings) {
            startY += ss.mHeight;
            mPaint.setColor(ss.mColor);
            mPaint.setTextSize(ss.mFontSize * Utils.FONT_SCALING);
            mPaint.setTypeface(ss.mFace);
            mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            canvas.drawText(ss.mText, midX - (ss.mWidth >> 1), startY - ss.mDescenderOffset, mPaint);
        }
    }

    void calculateManualScrollingPositions(Line firstLineOnscreen, Line currentLine, int currentY, boolean startOnscreen,boolean endOnscreen)
    {
        // If the end of the current line is on-screen, the linedown pixel position should be the start of the next line.
        // Otherwise, it should be 80% of the screen further down than it currently is.
        if(endOnscreen) {
            if ((firstLineOnscreen!=null) && (firstLineOnscreen.mNextLine != null))
                mLineDownPixel = firstLineOnscreen.mNextLine.mSongPixelPosition;
            else
                mLineDownPixel=mSongPixelPosition;
        }
        else
            mLineDownPixel = mSongPixelPosition+mPageUpDownScreenHeight;

        Line lineUpLine;
        if(startOnscreen)
        {
            if ((firstLineOnscreen!=null)&&(firstLineOnscreen.mPrevLine != null)) {
                mLineUpPixel = firstLineOnscreen.mPrevLine.mSongPixelPosition;
                lineUpLine=firstLineOnscreen.mPrevLine;
            }
            else {
                mLineUpPixel = mSongPixelPosition;
                lineUpLine=null;
            }
        }
        else
        {
            // If the start of the firstLineOnScreen is less than 80% of the screen away, scroll to it.
            // Otherwise, scroll up 80%.
            if((firstLineOnscreen!=null)&&(mSongPixelPosition-firstLineOnscreen.mSongPixelPosition<mPageUpDownScreenHeight)) {
                mLineUpPixel = firstLineOnscreen.mSongPixelPosition;
                lineUpLine=firstLineOnscreen;
            }
            else {
                mPageUpPixel=mLineUpPixel = mSongPixelPosition - mPageUpDownScreenHeight;
                lineUpLine=null;
            }
        }

        // If we managed to draw any of the last line, then page down should take us to the last line.
        // Is the END of the last line onscreen?
        if (currentY < mScreenHeight)
        {
            // Is the START of the last line onscreen?
            if((mSong.mLastLine!=null)&&(currentY-mSong.mLastLine.mLineMeasurements.mLineHeight>=0))
                // Yes? Then move the last line to the top of the screen
                mPageDownPixel = mSong.mLastLine.mSongPixelPosition;
            else
                // No? Then don't move.
                mPageDownPixel=mSongPixelPosition;
        }
        // Did we draw at least SOME of the last line?
        else if(currentLine == null)
        {
            // Does the entire last line fit onscreen?
            if((mSong.mLastLine!=null)&&(mSong.mLastLine.mLineMeasurements.mLineHeight<mScreenHeight))
                // Yes? Then move the last line to the top of the screen.
                mPageDownPixel=mSong.mLastLine.mSongPixelPosition;
            else
                // No? Then scroll 80%.
                mPageDownPixel = mSongPixelPosition+mPageUpDownScreenHeight;
        }
        // We haven't reached the last line yet.
        else
        {
            // Is the end of the first line drawn onscreen?
            if((currentLine!=null)&&(currentLine.mPrevLine!=null)&&(endOnscreen))
                // Yes? Then scroll to the last line drawn.
                mPageDownPixel=currentLine.mPrevLine.mSongPixelPosition;
            else
                // No? Then scroll 80%
                mPageDownPixel = mSongPixelPosition+mPageUpDownScreenHeight;
        }

        Line pageUpLine = currentLine = lineUpLine;
        if(pageUpLine!=null) {
            int totalHeight = mSongPixelPosition - lineUpLine.mSongPixelPosition;
            currentLine = currentLine.mPrevLine;
            mPageUpPixel=mSongPixelPosition;
            while (currentLine != null) {
                totalHeight += currentLine.mLineMeasurements.mLineHeight;
                if (totalHeight < mScreenHeight)
                    mPageUpPixel = currentLine.mSongPixelPosition;
                else
                    break;
                currentLine = currentLine.mPrevLine;
            }
            if(mPageUpPixel==mSongPixelPosition)
                mPageUpPixel=mSongPixelPosition-mPageUpDownScreenHeight;
        }

        mPageDownPixel=Math.min(mSongScrollEndPixel,mPageDownPixel);
        mLineDownPixel=Math.min(mSongScrollEndPixel,mLineDownPixel);
        mPageUpPixel=Math.max(0,mPageUpPixel);
        mLineUpPixel=Math.min(0,mLineUpPixel);
    }

    // Called back when the view is first created or its size changes.
    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        // Set the movement bounds for the ball
        // Can't use wider than 4096, as can't create bitmaps bigger than that.
        mScreenWidth = Math.min(w, 4096);
        mScreenHeight = h;
        if(mSong!=null)
            calculateScrollEnd();
        mPageUpDownScreenHeight=(int)(mScreenHeight*0.8);
    }

    public void showTempMessage(String message, int textSize, int textColor, Canvas canvas) {
        mPaint.setTextSize(textSize *Utils.FONT_SCALING);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        Rect outRect = new Rect();
        mPaint.getTextBounds(message, 0, message.length(), outRect);
        float textWidth=mPaint.measureText(message);
        int textHeight = outRect.height();
        float volumeControlWidth = textWidth + (POPUP_MARGIN * 2.0f);
        int volumeControlHeight = textHeight + (POPUP_MARGIN * 2);
        float x = (mScreenWidth - volumeControlWidth) / 2.0f;
        int y = (mScreenHeight - volumeControlHeight) / 2;
        mPaint.setColor(Color.BLACK);
        canvas.drawRect(x, y, x + volumeControlWidth, y + volumeControlHeight, mPaint);
        mPaint.setColor(Color.rgb(255,255,200));
        canvas.drawRect(x + 1, y + 1, x + (volumeControlWidth - 2), y + (volumeControlHeight - 2), mPaint);
        mPaint.setColor(textColor);
        canvas.drawText(message, (mScreenWidth - textWidth) / 2, ((mScreenHeight - textHeight) / 2) + textHeight, mPaint);
    }

    public void showSongTitle(Canvas canvas)
    {
        if((mSong==null)||(mSong.mSongTitleHeader==null))
            return;

        mPaint.setTextSize(mSong.mSongTitleHeader.mFontSize * Utils.FONT_SCALING);
        mPaint.setTypeface(mSong.mSongTitleHeader.mFace);
        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        mPaint.setColor(mSongTitleContrastBackground);
        canvas.drawText(mSong.mSongTitleHeader.mText,mSong.mSongTitleHeaderLocation.x,mSong.mSongTitleHeaderLocation.y,mPaint);

        canvas.save();
        canvas.clipRect(mBeatCountRect);
        mPaint.setColor(mSongTitleContrastBeatCounter);
        canvas.drawText(mSong.mSongTitleHeader.mText,mSong.mSongTitleHeaderLocation.x,mSong.mSongTitleHeaderLocation.y,mPaint);
        canvas.restore();

        if(mScrollIndicatorRect!=null)
        {
            canvas.save();
            canvas.clipRect(mScrollIndicatorRect);
            canvas.drawText(mSong.mSongTitleHeader.mText,mSong.mSongTitleHeaderLocation.x,mSong.mSongTitleHeaderLocation.y,mPaint);
            canvas.restore();
        }

        mPaint.setAlpha(255);
    }

    public void showComment(Canvas canvas)
    {
        if(mLastCommentEvent!=null)
        {
            mPaint.setTextSize(mLastCommentEvent.mScreenString.mFontSize * Utils.FONT_SCALING);
            mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            mPaint.setColor(Color.BLACK);
            canvas.drawRect(mLastCommentEvent.mPopupRect,mPaint);
            mPaint.setColor(Color.WHITE);
            canvas.drawRect(mLastCommentEvent.mPopupRect.left+1,mLastCommentEvent.mPopupRect.top+1,mLastCommentEvent.mPopupRect.right-1,mLastCommentEvent.mPopupRect.bottom-1,mPaint);
            mPaint.setColor(mCommentTextColor);
            mPaint.setAlpha(255);
            canvas.drawText(mLastCommentEvent.mComment.mText,mLastCommentEvent.mTextDrawLocation.x,mLastCommentEvent.mTextDrawLocation.y,mPaint);
        }
    }

    public boolean startToggle(MotionEvent e, boolean midiInitiated,int startState)
    {
        mStartState=startState;
        return startToggle(e,midiInitiated);
    }

    public boolean startToggle(MotionEvent e, boolean midiInitiated)
    {
        if(mSong==null)
            return true;
        if(mStartState<2)
        {
            if(mStartState==0)
                if(e!=null)
                    if(e.getY()>mScreenHeight*0.85f)
                        if((mSong.mNextSong!=null)&&(mSong.mNextSong.length()>0))
                        {
                            endSong(true);
                            return true;
                        }
            int oldStartState=mStartState;
            mStartState++;
            if (mStartState == 2)
            {
                if(mSong.mScrollingMode==ScrollingMode.Manual)
                {
                    // Start the count in.
                    if (mMetronomeThread != null)
                    {
                        if (!mMetronomeThread.isAlive())
                        {
                            if (mMetronomeBeats != 0)
                            {
                                mMetronomeThread.start();
                                return true;
                            }
                            else
                                return processTrackEvent();
                        }
                    }
                    else
                        return processTrackEvent();
                }
                else
                {
                    long time;
                    if (mUserHasScrolled)
                    {
                        mUserHasScrolled = false;
                        time = mSong.getTimeFromPixel(mSongPixelPosition);
                        setSongTime(time, false, false,false);
                    }
                    else
                    {
                        Log.d(BeatPrompterApplication.TAG, "Resuming, pause time=" + mPauseTime);
                        setSongTime(time=mPauseTime, false, false,true);
                    }
                    mApp.broadcastMessageToClients(new ToggleStartStopMessage(oldStartState,time));
                }
            }
            else
                mApp.broadcastMessageToClients(new ToggleStartStopMessage(oldStartState,0));
        }
        else
        {
            if(mScreenAction==ScreenAction.Volume)
            {
                if(e!=null) {
                    if (e.getY() < mScreenHeight * 0.5)
                        changeVolume(+5);
                    else if (e.getY() > mScreenHeight * 0.5)
                        changeVolume(-5);
                }
            }
            else if(mSong.mScrollingMode!=ScrollingMode.Manual) {
                if (mScreenAction == ScreenAction.Scroll)
                    pause(midiInitiated);
            }
        }
        invalidate();
        return true;
    }

    public void changeVolume(int amount)
    {
        if(mStartState==0)
            return;
        mCurrentVolume += amount;
        onVolumeChanged();
    }

    public void pause(boolean midiInitiated)
    {
        if(mScreenAction!=ScreenAction.Scroll)
            return;
        long nanoTime=System.nanoTime();
        mPauseTime=nanoTime-(mSongStartTime==0?nanoTime:mSongStartTime);
        mApp.broadcastMessageToClients(new ToggleStartStopMessage(mStartState,mPauseTime));
        mStartState--;
        if (mTrackMediaPlayer != null)
            if(mTrackMediaPlayer.isPlaying())
                mTrackMediaPlayer.pause();
        if(!midiInitiated)
            if(mSongDisplayActivity!=null)
                mSongDisplayActivity.onSongStop();
    }

    public void stop(boolean destroyed)
    {
        if(mStartState==2)
            pause(false);
        if(destroyed)
        {
            mApp.broadcastMessageToClients(new QuitSongMessage());
            if(mSong!=null)
                mSong.recycleGraphics();
            mSong=null;
            Task.stopTask(mMetronomeTask,mMetronomeThread);
            if(mTrackMediaPlayer!=null) {
                mTrackMediaPlayer.stop();
                mTrackMediaPlayer.release();
            }
            if(mSilenceMediaPlayer!=null) {
                mSilenceMediaPlayer.stop();
                mSilenceMediaPlayer.release();
            }
            if(mClickSoundPool!=null)
                mClickSoundPool.release();
            mTrackMediaPlayer=null;
            mSilenceMediaPlayer=null;
            mClickSoundPool=null;
            System.gc();
        }
    }

    public void processCommentEvent(CommentEvent event,long systemTime)
    {
        mLastCommentTime=systemTime;
        mLastCommentEvent=event;
    }

    public void processColorEvent(ColorEvent event)
    {
        if(event==mLastProcessedColorEvent)
            return;
        mBeatCounterColor=event.mBeatCounterColor;
        mScrollMarkerColor=event.mScrollMarkerColor;
        mSongTitleContrastBeatCounter=Utils.makeContrastingColour(mBeatCounterColor);
        int backgroundColor=event.mBackgroundColor;
        int pulseColor=mPulse?event.mPulseColor:event.mBackgroundColor;
        int bgR=Color.red(backgroundColor);
        int bgG=Color.green(backgroundColor);
        int bgB=Color.blue(backgroundColor);
        int pR=Color.red(pulseColor);
        int pG=Color.green(pulseColor);
        int pB=Color.blue(pulseColor);
        int rDiff=pR-bgR;
        int gDiff=pG-bgG;
        int bDiff=pB-bgB;
        for(int f=0;f<=100;++f)
        {
            double sineLookup = Utils.mSineLookup[(int) (90.0 * ((double)f/100.0))];
            int red = pR-(int)(sineLookup*(double)rDiff);
            int green = pG-(int)(sineLookup*(double)gDiff);
            int blue = pB-(int)(sineLookup*(double)bDiff);
            int color = Color.rgb(red, green, blue);
            mBackgroundColorLookup[f]=color;
        }
        mSongTitleContrastBackground=Utils.makeContrastingColour(mBackgroundColorLookup[100]);
    }

    private void processBeatEvent(BeatEvent event,boolean allowClick)
    {
        if(event==null)
            return;
        mNanosecondsPerBeat=Utils.nanosecondsPerBeat(event.mBPM);
        double beatWidth=((double)mScreenWidth/(double)(event.mBPB));
        int currentBeatCounterWidth=(int)(beatWidth*(double)(event.mBeat+1));
        if(event.mWillScrollOnBeat!=-1)
        {
            double thirdWidth=beatWidth/3;
            double thirdHeight=mSong.mBeatCounterHeight/3.0;
            int scrollIndicatorStart=(int)((beatWidth*(event.mWillScrollOnBeat))+thirdWidth);
            int scrollIndicatorEnd=(int)((beatWidth*(event.mWillScrollOnBeat+1))-thirdWidth);
            mScrollIndicatorRect=new Rect(scrollIndicatorStart,(int)thirdHeight,scrollIndicatorEnd,(int)(thirdHeight*2.0));
        }
        else
            mScrollIndicatorRect=null;
        mBeatCountRect=new Rect((int)(currentBeatCounterWidth-beatWidth),0,currentBeatCounterWidth,mSong.mBeatCounterHeight);
        mLastBeatTime=mSongStartTime+event.mEventTime;
        if((event.mClick)&&(mStartState==2)&&(mSong.mScrollingMode!=ScrollingMode.Manual)&&(allowClick))
            mClickSoundPool.play(mClickAudioID, 1.0f, 1.0f, 1, 0, 1.0f);
        if((mSongDisplayActivity!=null)/*&&(!event.mCount)*/)
            mSongDisplayActivity.onSongBeat(event.mBPM);
    }

    private void processPauseEvent(PauseEvent event)
    {
        if(event==null)
            return;
        mLastBeatTime=-1;
        int currentBeatCounterWidth=(int)(((double)mScreenWidth/(double)(event.mBeats-1))*(double)(event.mBeat));
        mBeatCountRect=new Rect(0,0,currentBeatCounterWidth,mSong.mBeatCounterHeight);
        mScrollIndicatorRect=new Rect(-1,-1,-1,-1);
    }

    private void processMIDIEvent(MIDIEvent event)
    {
        BeatPrompterApplication.mMIDIOutQueue.addAll(event.mMessages);
    }

    private void processLineEvent(LineEvent event)
    {
        if(mSong==null)
            return;
        mSong.mCurrentLine=event.mLine;
    }

    private boolean processTrackEvent()
    {
        if(mTrackMediaPlayer!=null)
        {
            Log.d(BeatPrompterApplication.TAG, "Track event hit: starting MediaPlayer");
            mTrackMediaPlayer.start();
            return true;
        }
        return false;
    }

    private boolean processEndEvent()
    {
        // End the song in beat mode, or if we're using a track in any other mode.
        boolean end=(mSong.mScrollingMode==ScrollingMode.Beat)||(mTrackMediaPlayer!=null);
        if(end)
            endSong(false);
        return end;
    }

    private void endSong(boolean skipped)
    {
        endSong(skipped,true);
    }
    private void endSong(boolean skipped,boolean naturalEnd)
     {
        if(mSongDisplayActivity!=null)
        {
            mSkipping=skipped;
            SongList.mSongEndedNaturally = naturalEnd;
            mStartState=0;
            mSongDisplayActivity = null;
            if(mSong!=null)
                mSong.recycleGraphics();
            mSong=null;
            if(mSongDisplayActivityHandler!=null)
            {
                mSongDisplayActivityHandler.obtainMessage(BeatPrompterApplication.END_SONG).sendToTarget();
                mSongDisplayActivityHandler=null;
            }
            System.gc();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        this.mGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    public double setSongTime(SetSongTimeMessage sstm)
    {
        if(mSong==null)
            return 0.0;
        Log.d(BeatPrompterApplication.TAG,"Setting time="+sstm.mTime);
        return setSongTime(sstm.mTime,true,false,true);
    }

    private void seekTrack(int time)
    {
        mTrackMediaPlayer.seekTo(time);
//        while(mTrackMediaPlayer.getCurrentPosition()!=time)
//        while(!getSeekCompleted())
/*            try {
                Thread.sleep(10);
            }
            catch(InterruptedException e)
            {
            }*/
    }

    public double setSongTime(long nano, boolean redraw, boolean broadcast, boolean setPixelPosition)
    {
        if(mSong==null)
            return 0.0;
        double bpm=0.0;
        // No time context in Manual mode.
        if(setPixelPosition)
            mSongPixelPosition=mSong.getPixelFromTime(nano);
//        if(mSong.mScrollingMode!=ScrollingMode.Manual)
        {
            if(mStartState<2)
                mPauseTime=nano;
            if (broadcast)
                mApp.broadcastMessageToClients(new SetSongTimeMessage(nano));
            mSong.setProgress(nano);
            processColorEvent(mSong.mCurrentEvent.mPrevColorEvent);
            if(mSong.mScrollingMode!=ScrollingMode.Manual) {
                BeatEvent prevBeatEvent = mSong.mCurrentEvent.mPrevBeatEvent;
                BeatEvent nextBeatEvent = mSong.mCurrentEvent.getNextBeatEvent();
                if (prevBeatEvent != null) {
                    bpm = prevBeatEvent.mBPM;
                    processBeatEvent(prevBeatEvent, nextBeatEvent != null);
                }
            }
            mSongStartTime = System.nanoTime() - nano;
            if(mSong.mScrollingMode!=ScrollingMode.Manual) {
                if (mTrackMediaPlayer != null) {
                    TrackEvent trackEvent = mSong.mCurrentEvent.mPrevTrackEvent;
                    if (trackEvent != null) {
                        int nTime = Utils.nanoToMilli(nano - trackEvent.mEventTime);
                        seekTrack(nTime);
//                    Log.d(BeatPrompterApplication.TAG, "Seek to=" + nTime);
                        if (mStartState == 2) {
                            Log.d(BeatPrompterApplication.TAG, "Starting MediaPlayer");
                            mTrackMediaPlayer.start();
                        }
                    } else {
                        Log.d(BeatPrompterApplication.TAG, "Seek to=0");
                        seekTrack(0);
                    }
                }
            }
            if (redraw)
                invalidate();
        }
        return bpm;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (mSong.mScrollingMode == ScrollingMode.Manual)
            if (mMetronomeThread != null)
                if(mStartState==2)
                    mMetronomeThread.interrupt();
        // Abort any active scroll animations and invalidate.
        if((mScreenAction==ScreenAction.Scroll)||(mSong.mScrollingMode==ScrollingMode.Manual))
            clearScrollTarget();
        mScroller.forceFinished(true);
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e)
    {
        startToggle(e,false);
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if(mScreenAction==ScreenAction.None)
            return false;
        if(mStartState==0)
            return false;
        if(mSong==null)
            return false;
        if((mScreenAction==ScreenAction.Scroll)||(mSong.mScrollingMode==ScrollingMode.Manual))
        {
            clearScrollTarget();
            mSongPixelPosition += (int) distanceY;
            mSongPixelPosition = Math.max(0, mSongPixelPosition);
            mSongPixelPosition = Math.min(mSongScrollEndPixel, mSongPixelPosition);
            pauseOnScrollStart();
//            if(mSong.mScrollingMode!=ScrollingMode.Manual)
                setSongTime(mSong.mCurrentLine.getTimeFromPixel(mSongPixelPosition), true,true,false);
        }
        else if(mScreenAction==ScreenAction.Volume)
        {
            mCurrentVolume+=(int)(distanceY/10.0);
            onVolumeChanged();
        }
        return true;
    }

    public void pauseOnScrollStart()
    {
        if(mSong.mScrollingMode==ScrollingMode.Manual)
            return;
        if(mScreenAction!=ScreenAction.Scroll)
            return;
        mApp.broadcastMessageToClients(new PauseOnScrollStartMessage());
        mUserHasScrolled = true;
        mStartState = 1;
        if (mTrackMediaPlayer != null)
            if (mTrackMediaPlayer.isPlaying())
            {
                Log.d(BeatPrompterApplication.TAG,"Pausing MediaPlayer");
                mTrackMediaPlayer.pause();
            }
        if(mSongDisplayActivity!=null)
            mSongDisplayActivity.onSongStop();
    }

    public void onVolumeChanged()
    {
        if(mTrackMediaPlayer!=null)
        {
            mCurrentVolume = Math.max(0, mCurrentVolume);
            mCurrentVolume = Math.min(100, mCurrentVolume);
            mTrackMediaPlayer.setVolume(0.01f * mCurrentVolume, 0.01f * mCurrentVolume);
            mLastTempMessageTime=System.nanoTime();
        }
    }

    @Override
    public void onLongPress(MotionEvent e)
    {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
    {
        if(mScreenAction==ScreenAction.None)
            return false;
        if(mStartState==0)
            return false;
        if(mSong==null)
            return false;
        if((mScreenAction==ScreenAction.Scroll)||(mSong.mScrollingMode==ScrollingMode.Manual))
        {
            clearScrollTarget();
            pauseOnScrollStart();
            mScroller.fling(0, mSongPixelPosition, 0, (int) -velocityY, 0, 0, 0, mSongScrollEndPixel);
        }
        else if(mScreenAction==ScreenAction.Volume)
            mScroller.fling(0, mCurrentVolume, 0, (int) velocityY, 0, 0, 0, 1000);
        return true;
    }

    private void changeThePage(boolean down)
    {
        if(mSongPixelPosition == mSongScrollEndPixel)
        {
            if ((++mEndSongByPedalCounter) == SONG_END_PEDAL_PRESSES)
                endSong(false);
            else
                mLastTempMessageTime=System.nanoTime();
        } else
            changePage(down);
    }

    public void onOtherPageDownActivated() {
        if(mStartState>0)
            onPageDownKeyPressed();
    }

    public void onPageDownKeyPressed() {
        if (mStartState < 2) {
            if (!startToggle(null, false) && (mSong.mScrollingMode == ScrollingMode.Manual))
                changeThePage(true);
        }
        else if (mSong.mScrollingMode == ScrollingMode.Manual)
        {
            if (mStartState == 2)
                changeThePage(true);
        }
        else
            changeVolume(+5);
    }

    public void onPageUpKeyPressed()
    {
        if(mStartState<2)
        {
            if (!startToggle(null,false)&&(mSong.mScrollingMode==ScrollingMode.Manual))
                changePage(false);
        }
        else if(mSong.mScrollingMode==ScrollingMode.Manual)
            changePage(false);
        else
            changeVolume(-5);
    }

    private void changeTheLine(boolean down) {
        if (mSongPixelPosition == mSongScrollEndPixel) {
            if ((++mEndSongByPedalCounter) == SONG_END_PEDAL_PRESSES)
                endSong(false);
            else
                mLastTempMessageTime = System.nanoTime();
        } else
            changeLine(down);
    }

    public void onLineDownKeyPressed()
    {
        if(mStartState<2)
        {
            if (!startToggle(null,false)&&(mSong.mScrollingMode==ScrollingMode.Manual))
                changeTheLine(true);
        }
        else if(mSong.mScrollingMode==ScrollingMode.Manual) {
            if (mStartState == 2)
                changeTheLine(true);
        }
        else
            changeVolume(+5);
    }

    public void onLineUpKeyPressed()
    {
        if(mStartState<2)
        {
            if (!startToggle(null,false)&&(mSong.mScrollingMode==ScrollingMode.Manual))
                changeLine(false);
        }
        else if(mSong.mScrollingMode==ScrollingMode.Manual)
            changeLine(false);
        else
            changeVolume(-5);
    }

    public void onLeftKeyPressed()
    {
        if(mStartState<2)
        {
            if (!startToggle(null,false)&&(mSong.mScrollingMode==ScrollingMode.Manual))
                changeVolume(-5);
        }
        else
            changeVolume(-5);
    }

    public void onRightKeyPressed()
    {
        if(mStartState<2)
        {
            if (!startToggle(null,false)&&(mSong.mScrollingMode==ScrollingMode.Manual))
                changeVolume(+5);
        }
        else
            changeVolume(+5);
    }

    public void changePage(boolean down)
    {
        if(mStartState==0)
            return;
        if((mTargetPixelPosition!=-1)&&(mTargetPixelPosition!=mSongPixelPosition))
            return;
        mTargetPixelPosition = down? mPageDownPixel:mPageUpPixel;
    }

    public void changeLine(boolean down)
    {
        if(mStartState==0)
            return;
        if((mTargetPixelPosition!=-1)&&(mTargetPixelPosition!=mSongPixelPosition))
            return;
        mTargetPixelPosition = down? mLineDownPixel:mLineUpPixel;
    }

    private void clearScrollTarget()
    {
        mTargetPixelPosition=-1;
        mTargetAcceleration=1;
    }

    public void setSongBeatPosition(int pointer,boolean midiInitiated)
    {
        long songTime=mSong.getMIDIBeatTime(pointer);
        setSongTime(songTime,true,midiInitiated,true);
    }

    public void startSong(boolean midiInitiated,boolean fromStart)
    {
        if(fromStart)
           setSongTime(0, true, midiInitiated,true);
        while(mStartState!=2)
          startToggle(null,midiInitiated);
    }

    public void stopSong(boolean midiInitiated)
    {
        if(mStartState==2)
            startToggle(null,midiInitiated);
    }

    boolean canYieldToMIDITrigger()
    {
        switch(mMIDITriggerSafetyCatch)
        {
            case Always:
                return true;
            case WhenAtTitleScreen:
                return mStartState==0;
            case WhenAtTitleScreenOrPaused:
                return mStartState<2 || (mSong!=null && mSong.mScrollingMode==ScrollingMode.Manual);
            case WhenAtTitleScreenOrPausedOrLastLine:
                return mStartState<2 || mSong==null || (mSong.mCurrentLine==null || mSong.mCurrentLine.mNextLine==null) || mSong.mScrollingMode==ScrollingMode.Manual;
            case Never:
            default:
                return false;
        }
    }

    class MetronomeTask extends Task
    {
        long mNanosecondsPerBeat;
        long mBeats;
        long mNextClickTime;
        MetronomeTask(double bpm, long beats)
        {
            super(true);
            mNanosecondsPerBeat= Utils.nanosecondsPerBeat(bpm);
            mBeats=beats;
        }
        public void doWork()
        {
            mMetronomeBeats=0;
            mNextClickTime=System.nanoTime();
            while (!getShouldStop()) {
                mClickSoundPool.play(mClickAudioID, 1.0f, 1.0f, 1, 0, 1.0f);
                if((--mBeats)==0)
                    stop();
                mNextClickTime+=mNanosecondsPerBeat;
                long wait=mNextClickTime-System.nanoTime();
                if (wait > 0) {
                    long millisecondsPerBeat = Utils.nanoToMilli(wait);
                    int nanosecondRemainder = (int) (wait - Utils.milliToNano(millisecondsPerBeat));
                    try{
                        Thread.sleep(millisecondsPerBeat, nanosecondRemainder);
                    }
                    catch(InterruptedException ie)
                    {
                        Log.d(BeatPrompterApplication.TAG, "Interrupted while waiting ... assuming resync attempt.", ie);
                        mNextClickTime=System.nanoTime();
                    }
                }
            }
            // If we're quitting this loop because we've run out of beats, then start the track.
            if(mBeats==0) {
                processTrackEvent();
            }
        }
    }
}
