package com.stevenfrew.beatprompter;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.Log;

import com.stevenfrew.beatprompter.cache.AudioFile;
import com.stevenfrew.beatprompter.cache.FileParseError;
import com.stevenfrew.beatprompter.cache.ImageFile;
import com.stevenfrew.beatprompter.cache.SongFile;
import com.stevenfrew.beatprompter.cache.Tag;
import com.stevenfrew.beatprompter.event.BaseEvent;
import com.stevenfrew.beatprompter.event.BeatEvent;
import com.stevenfrew.beatprompter.event.CancelEvent;
import com.stevenfrew.beatprompter.event.ColorEvent;
import com.stevenfrew.beatprompter.event.CommentEvent;
import com.stevenfrew.beatprompter.event.EndEvent;
import com.stevenfrew.beatprompter.event.LineEvent;
import com.stevenfrew.beatprompter.event.MIDIEvent;
import com.stevenfrew.beatprompter.event.PauseEvent;
import com.stevenfrew.beatprompter.event.TrackEvent;
import com.stevenfrew.beatprompter.midi.BeatBlock;
import com.stevenfrew.beatprompter.midi.EventOffset;
import com.stevenfrew.beatprompter.midi.Message;
import com.stevenfrew.beatprompter.midi.OutgoingMessage;
import com.stevenfrew.beatprompter.midi.ResolutionException;
import com.stevenfrew.beatprompter.midi.TriggerOutputContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Takes a SongFile and parses it into a Song.
 */
public class SongLoader {

    private final static int MAX_LINE_LENGTH=256;
    private final static int DEMO_LINE_COUNT=15;

    private SongLoadInfo mLoadingSongFile;
    private SongFile mSongFile;
    private int mCountInMin,mCountInMax,mCountInDefault;
    private int mBPMMin,mBPMMax,mBPMDefault;
    private int mBPLMin,mBPLMax,mBPLDefault;
    private int mBPBMin,mBPBMax,mBPBDefault;
    private ScrollingMode mUserChosenScrollMode;
    private ScrollingMode mCurrentScrollMode;
    private TriggerOutputContext mTriggerContext;
    private int mCountInPref;
    private int mDefaultTrackVolume;
    private byte mDefaultMIDIOutputChannel;
    private boolean mShowChords;
    private boolean mSendMidiClock;
    private int mBackgroundColour,mPulseColour,mBeatCounterColour,mScrollMarkerColour,mLyricColour,mChordColour,mAnnotationColour;
    private String mCustomCommentsUser;
    private boolean mIgnoreColorInfo;
    private MetronomeContext mMetronomeContext;
    private Handler mSongLoadHandler;
    private CancelEvent mCancelEvent;
    private boolean mRegistered;

    SongLoader(SongLoadInfo loadingSongFile, CancelEvent cancelEvent, Handler songLoadHandler, boolean registered)
    {
        mRegistered=registered;
        mLoadingSongFile=loadingSongFile;
        mSongFile=loadingSongFile.getSongFile();
        mUserChosenScrollMode=loadingSongFile.getScrollMode();
        mCancelEvent=cancelEvent;
        mSongLoadHandler=songLoadHandler;

        int countInOffset=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_offset));
        mCountInMin=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_min))+countInOffset;
        mCountInMax=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_max))+countInOffset;
        mCountInDefault=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_default))+countInOffset;

        int bpmOffset=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpm_offset));
        mBPMMin=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpm_min))+bpmOffset;
        mBPMMax=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpm_max))+bpmOffset;
        mBPMDefault=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpm_default))+bpmOffset;

        int bplOffset=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_offset));
        mBPLMin=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_min))+bplOffset;
        mBPLMax=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_max))+bplOffset;
        mBPLDefault=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_default))+bplOffset;

        int bpbOffset=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpb_offset));
        mBPBMin=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpb_min))+bpbOffset;
        mBPBMax=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpb_max))+bpbOffset;
        mBPBDefault=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpb_default))+bpbOffset;

        // OK, the "scrollMode" param is passed in here.
        // This might be what the user has explicitly chosen, i.e.
        // smooth mode or manual mode, chosen via the long-press play dialog.
        mCurrentScrollMode=mUserChosenScrollMode;
        // BUT, if the mode that has come in is "beat mode", and this is a mixed mode
        // song, we should be switching when we encounter beatstart/beatstop tags.
        if(mSongFile.mMixedMode && mCurrentScrollMode==ScrollingMode.Beat)
            // And if we ARE in mixed mode with switching allowed, we start in manual.
            mCurrentScrollMode= ScrollingMode.Manual;

        SharedPreferences sharedPref = BeatPrompterApplication.getPreferences();
        mTriggerContext= TriggerOutputContext.valueOf(sharedPref.getString(BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_key),BeatPrompterApplication.getResourceString(R.string.pref_sendMidiTriggerOnStart_defaultValue)));
        mCountInPref = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_default)));
        mCountInPref+=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_countIn_offset));
/*            int defaultPausePref = sharedPref.getInt(context.getString(R.string.pref_defaultPause_key), Integer.parseInt(context.getString(R.string.pref_defaultPause_default)));
            defaultPausePref+=Integer.parseInt(context.getString(R.string.pref_defaultPause_offset));*/
        mDefaultTrackVolume=sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultTrackVolume_key), Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultTrackVolume_default)));
        mDefaultTrackVolume+=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultTrackVolume_offset));
        int defaultMIDIOutputChannelPrefValue=sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_key),Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_defaultMIDIOutputChannel_default)));
        mDefaultMIDIOutputChannel= Message.getChannelFromBitmask(defaultMIDIOutputChannelPrefValue);
        mShowChords=sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showChords_key), Boolean.parseBoolean(BeatPrompterApplication.getResourceString(R.string.pref_showChords_defaultValue)));
        mSendMidiClock = sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_sendMidi_key), false);
        mBackgroundColour = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_backgroundColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_backgroundColor_default)));
        mPulseColour = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_pulseColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_pulseColor_default)));
        mBeatCounterColour = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_beatCounterColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_beatCounterColor_default)));
        mScrollMarkerColour = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_scrollMarkerColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_scrollMarkerColor_default)));
        mLyricColour = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_lyricColor_key),Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_lyricColor_default)));
        mChordColour = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_chordColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_chordColor_default)));
        mAnnotationColour = sharedPref.getInt(BeatPrompterApplication.getResourceString(R.string.pref_annotationColor_key), Color.parseColor(BeatPrompterApplication.getResourceString(R.string.pref_annotationColor_default)));
        mCustomCommentsUser=sharedPref.getString(BeatPrompterApplication.getResourceString(R.string.pref_customComments_key), BeatPrompterApplication.getResourceString(R.string.pref_customComments_defaultValue));
        mBackgroundColour|=0xff000000;
        mAnnotationColour|=0xff000000;
        mPulseColour|=0xff000000;
        mBeatCounterColour|=0xff000000;
        mLyricColour|=0xff000000;
        mChordColour|=0xff000000;
        mIgnoreColorInfo=sharedPref.getBoolean(BeatPrompterApplication.getResourceString(R.string.pref_ignoreColorInfo_key), Boolean.parseBoolean(BeatPrompterApplication.getResourceString(R.string.pref_ignoreColorInfo_defaultValue)));
        try {
            mMetronomeContext = MetronomeContext.valueOf(sharedPref.getString(BeatPrompterApplication.getResourceString(R.string.pref_metronome_key), BeatPrompterApplication.getResourceString(R.string.pref_metronome_defaultValue)));
        }
        catch(Exception e)
        {
            // backward compatibility with old shite values.
            mMetronomeContext=MetronomeContext.Off;
        }
    }

    public Song load() throws IOException
    {
        int scrollBeatMin=1,scrollBeatDefault=4;
        ArrayList<OutgoingMessage> initialMIDIMessages=new ArrayList<>();
        ArrayList<FileParseError> errors=new ArrayList<>();
        boolean stopAddingStartupItems=false;

        String chosenTrack=mLoadingSongFile.getTrack();
        SmoothScrollingTimings sst=mSongFile.getTimePerLineAndBar(chosenTrack,null,null);
        long timePerLine=sst.getTimePerLine();
        long timePerBar=sst.getTimePerBar();

        if((timePerLine<0)||(timePerBar<0)) {
            errors.add(new FileParseError(null, BeatPrompterApplication.getResourceString(R.string.pauseLongerThanSong)));
            sst.setTimePerLine(-timePerLine);
            sst.setTimePerBar(-timePerBar);
        }

        BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(mSongFile.mFile)));
        try
        {
            String line;

            ArrayList<Tag> tagsOut=new ArrayList<>();
            HashSet<String> tagsSet=new HashSet<>();

            // ONE SHOT
            AudioFile chosenAudioFile=null;
            int chosenAudioVolume=100;
            int count=mCountInPref;
            long trackOffset=0;
            // TIME
            double bpm=mSongFile.mBPM==0?120:mSongFile.mBPM;
            int bpb=4;
            int initialBPB=4;
            boolean initialBPBSet=false;
            int bpl=1;
            int beatsToAdjust=0;
            ArrayList<BeatEvent> rolloverBeats=new ArrayList<>();
            int pauseTime;
            int scrollBeat=bpb;
            BeatBlock lastBeatBlock =null;
            // COMMENT
            ArrayList<Comment> comments=new ArrayList<>();
            ArrayList<BeatBlock> beatBlocks=new ArrayList<>();
            String commentAudience;
            ImageFile lineImage=null;

            boolean metronomeOn=mMetronomeContext==MetronomeContext.On;
            if(mMetronomeContext==MetronomeContext.OnWhenNoTrack && (chosenTrack==null || chosenTrack.length()==0))
                metronomeOn=true;

            int currentBeat=0;
            long nanosecondsPerBeat;

            ImageScalingMode imageScalingMode= ImageScalingMode.Stretch;
            long currentTime=0;
            int midiBeatCounter=0;
            BaseEvent firstEvent=null;
            BaseEvent lastEvent=null;
            Line firstLine=null;

            // There must ALWAYS be a style and time event at the start, before any line events.
            // Create them from defaults even if there are no relevant tags in the file.
            boolean createColorEvent=true;
            int lineCounter=0;
            int displayLineCounter=0;
            mSongLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_PROCESSED,0,mSongFile.mLines).sendToTarget();
            while(((line=br.readLine())!=null)&&(!mCancelEvent.isCancelled()))
            {
                line=line.trim();
                pauseTime=0;
                lineCounter++;
                // Ignore comments.
                if(!line.startsWith("#"))
                {
                    if(line.length()>MAX_LINE_LENGTH)
                    {
                        line=line.substring(0,MAX_LINE_LENGTH);
                        errors.add(new FileParseError(null, BeatPrompterApplication.getResourceString(R.string.lineTooLong,lineCounter,MAX_LINE_LENGTH)));
                    }
                    tagsOut.clear();
                    String strippedLine=Tag.extractTags(line, lineCounter, tagsOut);
                    // Replace stupid unicode BOM character
                    strippedLine = strippedLine.replace("\uFEFF", "");
                    boolean chordsFound=false;
                    int scrollbeatOffset=0;
                    for(Tag tag:tagsOut)
                    {
                        // Not bothered about chords at the moment.
                        if(tag.mChordTag) {
                            chordsFound = true;
                            continue;
                        }

                        if((Tag.COLOR_TAGS.contains(tag.mName))&&(!mIgnoreColorInfo))
                            createColorEvent=true;
                        if((Tag.ONE_SHOT_TAGS.contains(tag.mName))&&(tagsSet.contains(tag.mName)))
                            errors.add(new FileParseError(tag,BeatPrompterApplication.getResourceString(R.string.oneShotTagDefinedTwice,tag.mName)));
                        commentAudience=null;
                        if(tag.mName.startsWith("c@"))
                        {
                            commentAudience = tag.mName.substring(1);
                            tag.mName="c";
                        }
                        else if(tag.mName.startsWith("comment@"))
                        {
                            commentAudience = tag.mName.substring(7);
                            tag.mName="c";
                        }
                        switch(tag.mName)
                        {
                            case "image":
                                if(lineImage!=null)
                                {
                                    errors.add(new FileParseError(lineCounter, BeatPrompterApplication.getResourceString(R.string.multiple_images_in_one_line)));
                                    break;
                                }
                                String imageName=tag.mValue;
                                int colonindex=imageName.indexOf(":");
                                imageScalingMode=ImageScalingMode.Stretch;
                                if((colonindex!=-1)&&(colonindex<imageName.length()-1))
                                {
                                    String strScalingMode=imageName.substring(colonindex+1);
                                    imageName=imageName.substring(0,colonindex);
                                    if(strScalingMode.equalsIgnoreCase("stretch"))
                                        imageScalingMode=ImageScalingMode.Stretch;
                                    else if(strScalingMode.equalsIgnoreCase("original"))
                                        imageScalingMode=ImageScalingMode.Original;
                                    else
                                        errors.add(new FileParseError(lineCounter,BeatPrompterApplication.getResourceString(R.string.unknown_image_scaling_mode)));
                                }
                                String image=new File(imageName).getName();
                                File imageFile;
                                ImageFile mappedImage=SongList.mCachedCloudFiles.getMappedImageFilename(image,null);
                                if(mappedImage==null)
                                    errors.add(new FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindImageFile,image)));
                                else
                                {
                                    imageFile = new File(mSongFile.mFile.getParent(), mappedImage.mFile.getName());
                                    if (!imageFile.exists())
                                        errors.add(new FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindImageFile,image)));
                                }
                                lineImage=mappedImage;
                                break;
                            case "track":
                            case "audio":
                            case "musicpath":
                                String trackName=tag.mValue;
                                int volume=mDefaultTrackVolume;
                                int trackcolonindex=trackName.indexOf(":");
                                if((trackcolonindex!=-1)&&(trackcolonindex<trackName.length()-1))
                                {
                                    String strVolume=trackName.substring(trackcolonindex+1);
                                    trackName=trackName.substring(0,trackcolonindex);
                                    try
                                    {
                                        int tryvolume = Integer.parseInt(strVolume);
                                        if((tryvolume<0)||(tryvolume>100))
                                            errors.add(new FileParseError(lineCounter,BeatPrompterApplication.getResourceString(R.string.badAudioVolume)));
                                        else
                                            volume=(int)((double)volume*((double)tryvolume/100.0));
                                    }
                                    catch(NumberFormatException nfe)
                                    {
                                        errors.add(new FileParseError(lineCounter,BeatPrompterApplication.getResourceString(R.string.badAudioVolume)));
                                    }
                                }
                                String track=new File(trackName).getName();
                                File trackFile=null;
                                AudioFile mappedTrack=SongList.mCachedCloudFiles.getMappedAudioFilename(track,null);
                                if(mappedTrack==null)
                                    errors.add(new FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindAudioFile,track)));
                                else
                                {
                                    trackFile = new File(mSongFile.mFile.getParent(), mappedTrack.mFile.getName());
                                    if (!trackFile.exists()) {
                                        errors.add(new FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindAudioFile,track)));
                                        trackFile = null;
                                    }
                                }
                                if((trackFile!=null)&&(chosenTrack!=null)&&(track.equalsIgnoreCase(chosenTrack)))
                                {
                                    chosenAudioFile=mappedTrack;
                                    chosenAudioVolume=volume;
                                }
                                break;
                            case "send_midi_clock":
                                mSendMidiClock=true;
                                break;
                            case "count":
                            case "countin":
                                count=Tag.getIntegerValueFromTag(tag, mCountInMin, mCountInMax, mCountInDefault, errors);
                                break;
//                            case "trackoffset":
//                                trackOffset=Tag.getLongValueFromTag(tag, trackOffsetMin, trackOffsetMax, trackOffsetDefault, errors);
//                                break;
                            case "backgroundcolour":
                            case "backgroundcolor":
                            case "bgcolour":
                            case "bgcolor":
                                mBackgroundColour=Tag.getColourValueFromTag(tag, mBackgroundColour, errors);
                                break;
                            case "pulsecolour":
                            case "pulsecolor":
                            case "beatcolour":
                            case "beatcolor":
                                mPulseColour=Tag.getColourValueFromTag(tag, mPulseColour, errors);
                                break;
                            case "lyriccolour":
                            case "lyriccolor":
                            case "lyricscolour":
                            case "lyricscolor":
                                mLyricColour=Tag.getColourValueFromTag(tag, mLyricColour, errors);
                                break;
                            case "chordcolour":
                            case "chordcolor":
                                mChordColour=Tag.getColourValueFromTag(tag, mChordColour, errors);
                                break;
                            case "beatcountercolour":
                            case "beatcountercolor":
                                mBeatCounterColour=Tag.getColourValueFromTag(tag, mBeatCounterColour, errors);
                                break;
                            case "bpm":
                            case "metronome":
                            case "beatsperminute":
                                bpm=Tag.getDoubleValueFromTag(tag, mBPMMin, mBPMMax, mBPMDefault, errors);
                                break;
                            case "bpb":
                            case "beatsperbar":
                                int prevScrollBeatDiff=bpb-scrollBeat;
                                bpb=Tag.getIntegerValueFromTag(tag, mBPBMin, mBPBMax, mBPBDefault, errors);
                                if(!initialBPBSet) {
                                    initialBPB = bpb;
                                    initialBPBSet=true;
                                }
                                scrollBeatDefault=bpb;
                                if(bpb-prevScrollBeatDiff>0)
                                    scrollBeat=bpb-prevScrollBeatDiff;
                                if(scrollBeat>bpb)
                                    scrollBeat=bpb;
                                break;
                            case "bpl":
                            case "barsperline":
                                bpl=Tag.getIntegerValueFromTag(tag, mBPLMin, mBPLMax, mBPLDefault, errors);
                                break;
                            case "scrollbeat":
                            case "sb":
                                scrollBeat=Tag.getIntegerValueFromTag(tag, scrollBeatMin, bpb, scrollBeatDefault, errors);
                                if(scrollBeat>bpb)
                                    scrollBeat=bpb;
                                break;
                            case "comment":
                            case "c":
                            case "comment_box":
                            case "cb":
                            case "comment_italic":
                            case "ci":
                                Comment comment = new Comment(tag.mValue, commentAudience);
                                if(stopAddingStartupItems)
                                {
                                    CommentEvent ce=new CommentEvent(currentTime,comment);
                                    if(firstEvent==null)
                                        firstEvent=ce;
                                    else
                                        lastEvent.add(ce);
                                    lastEvent=ce;
                                }
                                else
                                {
                                    if (comment.isIntendedFor(mCustomCommentsUser))
                                        comments.add(comment);
                                }
                                break;
                            case "pause":
                                pauseTime=Tag.getDurationValueFromTag(tag,1000,60*60*1000,0,false,errors);
                                break;
                            case "midi_song_select_trigger":
                            case "midi_program_change_trigger":
                                // Don't need the value after the song is loaded, we're just showing informational
                                // errors about bad formatting.
                                Tag.verifySongTriggerFromTag(tag,errors);
                                break;
                            case "time":
                            case "tag":
                            case "bars":
                            case "soh":
                            case "eoh":
                            case "b":
                            case "title":
                            case "t":
                            case "artist":
                            case "a":
                            case "subtitle":
                            case "st":
                            case "key":
                                // Dealt with in pre-parsing.
                                break;
                            case "start_of_chorus":
                            case "end_of_chorus":
                            case "start_of_tab":
                            case "end_of_tab":
                            case "soc":
                            case "eoc":
                            case "sot":
                            case "eot":
                            case "define":
                            case "textfont":
                            case "tf":
                            case "textsize":
                            case "ts":
                            case "chordfont":
                            case "cf":
                            case "chordsize":
                            case "cs":
                            case "no_grid":
                            case "ng":
                            case "grid":
                            case "g":
                            case "titles":
                            case "new_page":
                            case "np":
                            case "new_physical_page":
                            case "npp":
                            case "columns":
                            case "col":
                            case "column_break":
                            case "colb":
                            case "pagetype":
                                // ChordPro stuff we're not supporting.
                                break;
                            case "capo":
                            case "zoom-android":
                            case "zoom":
                            case "tempo":
                            case "tempo-android":
                            case "instrument":
                            case "tuning":
                                // SongBook stuff we're not supporting.
                                break;
                            case"beatstart":
                                break;
                            case "beatstop":
                                break;
                            default:
                                try {
                                    if((displayLineCounter>DEMO_LINE_COUNT)&&(!mRegistered))
                                        // NO MIDI FOR YOU
                                        break;
                                    MIDIEvent me = Tag.getMIDIEventFromTag(currentTime,tag, SongList.getMIDIAliases(), mDefaultMIDIOutputChannel, errors);
                                    if (me!=null)
                                    {
                                        if(stopAddingStartupItems) {
                                            if (firstEvent == null)
                                                firstEvent = me;
                                            else
                                                lastEvent.add(me);
                                            lastEvent = me;
                                        }
                                        else
                                        {
                                            initialMIDIMessages.addAll(me.mMessages);
                                            if(me.mOffset!=null)
                                                errors.add(new FileParseError(tag,BeatPrompterApplication.getResourceString(R.string.midi_offset_before_first_line)));
                                        }
                                    }
                                }
                                catch(Exception e)
                                {
                                    errors.add(new FileParseError(tag,e.getMessage()));
                                }
                                break;
                        }
                        tagsSet.add(tag.mName);
                    }
                    boolean createLine=false;
                    boolean allowBlankLine=false;
                    if((!mShowChords)&&(chordsFound))
                    {
                        chordsFound=false;
                        allowBlankLine=true;
                        ArrayList<Tag> noChordsTags=new ArrayList<>();
                        for(Tag tag:tagsOut)
                            if(!tag.mChordTag)
                                noChordsTags.add(tag);
                        tagsOut=noChordsTags;
                    }
                    if((strippedLine.trim().length()>0)||(allowBlankLine)||(chordsFound)||(lineImage!=null))
                        createLine=true;

                    // Contains only tags? Or contains nothing? Don't use it as a blank line.
                    if((createLine)||(pauseTime>0))
                    {
                        // We definitely have a line event!
                        // Deal with style/time/comment events now.
                        if(createColorEvent)
                        {
                            ColorEvent styleEvent=new ColorEvent(currentTime,mBackgroundColour,mPulseColour,mLyricColour,mChordColour,mAnnotationColour,mBeatCounterColour,mScrollMarkerColour);
                            if((currentTime==0)&&(firstEvent!=null))
                            {
                                // First event should ALWAYS be a color event.
                                BaseEvent oldFirstEvent=firstEvent;
                                firstEvent=styleEvent;
                                firstEvent.add(oldFirstEvent);
                            }
                            else {
                                if (firstEvent == null)
                                    firstEvent = styleEvent;
                                else
                                    lastEvent.add(styleEvent);
                                lastEvent = styleEvent;
                            }
                            createColorEvent=false;
                        }

                        if(lastEvent.mPrevLineEvent==null)
                        {
                            // There haven't been any line events yet.
                            // So the comments that have been gathered up until now
                            // can just be shown on the song startup screen.
                            stopAddingStartupItems=true;
                        }

                        int bars=0;
                        boolean commasFound=false;
                        while(strippedLine.startsWith(","))
                        {
                            commasFound=true;
                            strippedLine=strippedLine.substring(1);
                            for(Tag tag:tagsOut)
                                if(tag.mPosition>0)
                                    tag.mPosition--;
                            bars++;
                        }
                        bars=Math.max(1, bars);

                        int bpbThisLine=bpb;
                        while((strippedLine.endsWith(">"))||(strippedLine.endsWith("<"))/*||(strippedLine.endsWith("+"))||(strippedLine.endsWith("_"))*/)
                        {
                            if(strippedLine.endsWith(">"))
                                scrollbeatOffset++;
                            else if(strippedLine.endsWith("<"))
                                scrollbeatOffset--;
/*                            else if(strippedLine.endsWith("+")) {
                                scrollbeatOffset++;
                                bpbThisLine++;
                            }
                            else if(strippedLine.endsWith("_")) {
                                scrollbeatOffset--;
                                bpbThisLine--;
                            }*/
                            strippedLine=strippedLine.substring(0,strippedLine.length()-1);
                            for(Tag tag:tagsOut)
                                if(tag.mPosition>strippedLine.length())
                                    tag.mPosition--;
                        }

                        if((scrollbeatOffset<-bpbThisLine)||(scrollbeatOffset>=bpbThisLine))
                        {
                            errors.add(new FileParseError(lineCounter,BeatPrompterApplication.getResourceString(R.string.scrollbeatOffTheMap)));
                            scrollbeatOffset=0;
                        }
                        if(!commasFound)
                            bars=bpl;

                        if((lineImage!=null)&&((strippedLine.trim().length()>0)||(chordsFound)))
                            errors.add(new FileParseError(lineCounter,BeatPrompterApplication.getResourceString(R.string.text_found_with_image)));

                        if((strippedLine.trim().length()==0)&&(!chordsFound))
                            strippedLine="▼";

//                        if((firstLine==null)&&(smoothScrolling))
//                            pauseTime+=defaultPausePref*1000;

                        if(createLine)
                        {
                            displayLineCounter++;
                            if((displayLineCounter>DEMO_LINE_COUNT)&&(!mRegistered))
                            {
                                tagsOut=new ArrayList<>();
                                strippedLine = BeatPrompterApplication.getResourceString(R.string.please_buy);
                                lineImage=null;
                            }
                            Line lastLine = null;
                            if (lastEvent.mPrevLineEvent != null)
                                lastLine = lastEvent.mPrevLineEvent.mLine;
                            int lastScrollbeatOffset=0;
                            int lastBPB=bpbThisLine;
                            int lastScrollbeat=scrollBeatDefault;
                            if (lastLine != null) {
                                lastBPB=lastLine.mBPB;
                                lastScrollbeatOffset = lastLine.mScrollbeatOffset;
                                lastScrollbeat=lastLine.mScrollbeat;
                            }
                            int scrollbeatDifference=(scrollBeat-bpbThisLine)-(lastScrollbeat-lastBPB);

                            Line lineObj;
                            if(lineImage!=null) {
                                lineObj = new ImageLine(lineImage, imageScalingMode, tagsOut, bars, lastEvent.mPrevColorEvent, bpbThisLine, scrollBeat, scrollbeatOffset,mCurrentScrollMode, errors);
                                lineImage=null;
                            }
                            else
                                lineObj= new TextLine(strippedLine,tagsOut, bars, lastEvent.mPrevColorEvent, bpbThisLine, scrollBeat, scrollbeatOffset, mCurrentScrollMode,errors);

                            bars=lineObj.mBars;
                            int beatsForThisLine=bpbThisLine*bars;
                            int simpleBeatsForThisLine=beatsForThisLine;
                            beatsForThisLine+=scrollbeatOffset;
                            beatsForThisLine+=scrollbeatDifference;
                            beatsForThisLine-=lastScrollbeatOffset;

                            if(bpm>0)
                                nanosecondsPerBeat=Utils.nanosecondsPerBeat(bpm);
                            else
                                nanosecondsPerBeat=0;

                            long totalLineTime;
                            if(pauseTime>0)
                                totalLineTime=Utils.milliToNano(pauseTime);
                            else
                                totalLineTime=beatsForThisLine * nanosecondsPerBeat;
                            if((totalLineTime==0)||(mCurrentScrollMode==ScrollingMode.Smooth))
                                totalLineTime=timePerBar*bars;

                            LineEvent lineEvent = new LineEvent(currentTime, totalLineTime);
                            if (lastLine != null)
                                lastLine.insertAfter(lineObj);
                            lineObj.mLineEvent = lineEvent;
                            lineEvent.mLine = lineObj;
                            if (firstLine == null)
                                firstLine = lineObj;
                            lastEvent.insertEvent(lineEvent);

                            // generate beats ...

                            // if a pause is specified on a line, it replaces the actual beats for that line.
                            if(pauseTime>0)
                            {
                                currentTime=generatePause(pauseTime,lastEvent,currentTime);
                                lastEvent=lastEvent.getLastEvent();
                                lineEvent.mLine.mYStartScrollTime = currentTime-nanosecondsPerBeat;
                                lineEvent.mLine.mYStopScrollTime = currentTime;
                            }
                            else if((bpm>0)&&(mCurrentScrollMode!=ScrollingMode.Smooth))
                            {
                                boolean finished = false;
                                int beatThatWeWillScrollOn=0;
                                int rolloverBeatCount=rolloverBeats.size();
                                int beatsToAdjustCount=beatsToAdjust;
                                if(beatsToAdjust>0)
                                {
                                    // We have N beats to adjust.
                                    // For the previous N beatevents, set the BPB to the new BPB.
                                    BeatEvent lastBeatEvent=lastEvent.mPrevBeatEvent;
                                    while((lastBeatEvent!=null)&&(beatsToAdjust>0))
                                    {
                                        lastBeatEvent.mBPB=bpbThisLine;
                                        beatsToAdjust--;
                                        if(lastBeatEvent.mPrevEvent!=null)
                                            lastBeatEvent=lastBeatEvent.mPrevEvent.mPrevBeatEvent;
                                        else
                                            lastBeatEvent=null;
                                    }
                                    beatsToAdjust=0;
                                }

                                for (int currentBarBeat = 0; (!finished) && (currentBarBeat < beatsForThisLine); ++currentBarBeat) {
                                    int beatsRemaining = beatsForThisLine - currentBarBeat;
                                    if (beatsRemaining > bpbThisLine)
                                        beatThatWeWillScrollOn = -1;
                                    else
                                        beatThatWeWillScrollOn = (currentBeat + (beatsRemaining - 1))%bpbThisLine;
                                    BeatEvent beatEvent;
                                    int rolloverBPB=0;
                                    long rolloverBeatLength=0;
                                    if(rolloverBeats.isEmpty())
                                        beatEvent= new BeatEvent(currentTime, bpm, bpbThisLine, bpl, currentBeat, metronomeOn, beatThatWeWillScrollOn);
                                    else {
                                        beatEvent = rolloverBeats.get(0);
                                        beatEvent.mWillScrollOnBeat=beatThatWeWillScrollOn;
                                        rolloverBPB=beatEvent.mBPB;
                                        rolloverBeatLength=Utils.nanosecondsPerBeat(beatEvent.mBPM);
                                        rolloverBeats.remove(0);
                                    }
                                    lastEvent.insertEvent(beatEvent);
                                    long beatTimeLength=(rolloverBeatLength==0?nanosecondsPerBeat:rolloverBeatLength);
                                    double nanoPerBeat=beatTimeLength/4.0;
                                    // generate MIDI beats.
                                    if((lastBeatBlock ==null)||(nanoPerBeat!= lastBeatBlock.getNanoPerBeat())) {
                                        BeatBlock beatBlock = lastBeatBlock = new BeatBlock(beatEvent.mEventTime, midiBeatCounter++, nanoPerBeat);
                                        beatBlocks.add(beatBlock);
                                    }

                                    if (currentBarBeat == beatsForThisLine - 1) {
                                        lineEvent.mLine.mYStartScrollTime = mCurrentScrollMode==ScrollingMode.Smooth?lineEvent.mEventTime:currentTime;
                                        lineEvent.mLine.mYStopScrollTime = currentTime + nanosecondsPerBeat;
                                        finished = true;
                                    }
                                    currentTime += beatTimeLength;
                                    currentBeat++;
                                    if (currentBeat == (rolloverBPB>0?rolloverBPB:bpbThisLine))
                                        currentBeat = 0;
                                }

                                beatsForThisLine-=rolloverBeatCount;
                                beatsForThisLine+=beatsToAdjustCount;
                                if(beatsForThisLine>simpleBeatsForThisLine)
                                {
                                    // We need to store some information so that the next line can adjust the rollover beats.
                                    beatsToAdjust=beatsForThisLine-simpleBeatsForThisLine;
                                }
                                else if(beatsForThisLine<simpleBeatsForThisLine)
                                {
                                    // We need to generate a few beats to store for the next line to use.
                                    rolloverBeats.clear();
                                    int rolloverCurrentBeat=currentBeat;
                                    long rolloverCurrentTime=currentTime;
                                    for(int f=beatsForThisLine;f<simpleBeatsForThisLine;++f)
                                    {
                                        rolloverBeats.add(new BeatEvent(rolloverCurrentTime, bpm, bpbThisLine, bpl, rolloverCurrentBeat++, metronomeOn, beatThatWeWillScrollOn));
                                        rolloverCurrentTime += nanosecondsPerBeat;
                                        if (rolloverCurrentBeat == bpbThisLine)
                                            rolloverCurrentBeat = 0;
                                    }
                                }
                            }
                            else
                            {
                                lineEvent.mLine.mYStartScrollTime = currentTime;
                                currentTime += totalLineTime;
                                lineEvent.mLine.mYStopScrollTime = currentTime;
                            }
                        }
                        else if(pauseTime>0)
                            currentTime=generatePause(pauseTime,lastEvent,currentTime);

                        lastEvent=lastEvent.getLastEvent();
                    }
                }
                mSongLoadHandler.obtainMessage(EventHandler.SONG_LOAD_LINE_READ,lineCounter,mSongFile.mLines).sendToTarget();
            }

            long countTime = 0;
            // Create count events
            if((bpm>0)&&(mCurrentScrollMode!=ScrollingMode.Manual))
            {
                BeatEvent firstBeatEvent = firstEvent.getFirstBeatEvent();
                double countbpm=firstBeatEvent==null?120.0:firstBeatEvent.mBPM;
                int countbpb=firstBeatEvent==null?4:firstBeatEvent.mBPB;
                int countbpl=firstBeatEvent==null?1:firstBeatEvent.mBPL;
                BaseEvent insertAfterEvent = firstEvent;
                if (count > 0) {
                    long nanoPerBeat = Utils.nanosecondsPerBeat(countbpm);
                    for (int f = 0; f < count; ++f)
                        for (int g = 0; g < countbpb; ++g)
                        {
                            BeatEvent countEvent = new BeatEvent(countTime,countbpm, countbpb, countbpl, g, mMetronomeContext==MetronomeContext.DuringCountIn || metronomeOn,(f==count-1?bpb-1:-1));
                            insertAfterEvent.insertAfter(countEvent);
                            insertAfterEvent = countEvent;
                            countTime += nanoPerBeat;
                        }
                    insertAfterEvent.offsetLaterEvents(countTime);
                } else {
                    BeatEvent baseBeatEvent = new BeatEvent(0, countbpm, countbpb, countbpl, countbpb, false,-1);
                    firstEvent.insertAfter(baseBeatEvent);
                }
            }
            TrackEvent trackEvent=null;
            if(chosenAudioFile!=null)
            {
                trackOffset=Utils.milliToNano((int)trackOffset); // milli to nano
                trackOffset+=countTime;
                BaseEvent eventBefore=firstEvent.findEventOnOrBefore(trackOffset);
                trackEvent = new TrackEvent(trackOffset<0?0:trackOffset);
                eventBefore.insertAfter(trackEvent);
                if(trackOffset<0)
                    trackEvent.offsetLaterEvents(Math.abs(trackOffset));
            }
            if((bpm>0)&&(mCurrentScrollMode==ScrollingMode.Beat))
            {
                // Last Y scroll should never happen. No point scrolling last line offscreen.
                Line mLastLine = firstLine.getLastLine();
                if (mLastLine != null)
                    mLastLine.mYStartScrollTime = mLastLine.mYStopScrollTime = Long.MAX_VALUE;
            }

            // Nothing at all in the song file? We at least want the colors set right.
            if(firstEvent==null)
                firstEvent=new ColorEvent(currentTime,mBackgroundColour,mPulseColour,mLyricColour,mChordColour,mAnnotationColour,mBeatCounterColour,mScrollMarkerColour);

            BaseEvent reallyTheLastEvent=firstEvent.getLastEvent();
            // In beat mode, or in any other mode where we're using a backing track, let's have an end event.
            if((trackEvent!=null)||(mCurrentScrollMode!=ScrollingMode.Manual))
            {
                long trackEndTime=0;
                if(trackEvent!=null)
                    trackEndTime=trackEvent.mEventTime+sst.getTrackLength();
                // The end event will be where the final beat occurs.
                // But there is a chance that the audio track is longer than that.
                EndEvent endEvent = new EndEvent(Math.max(currentTime,trackEndTime));
                reallyTheLastEvent.add(endEvent);
            }

            if((mTriggerContext== TriggerOutputContext.Always)||(mTriggerContext== TriggerOutputContext.ManualStartOnly && !mLoadingSongFile.getStartedByMIDITrigger()))
            {
                if(mSongFile.mProgramChangeTrigger!=null)
                    if(mSongFile.mProgramChangeTrigger.isSendable())
                        try {
                            initialMIDIMessages.addAll(mSongFile.mProgramChangeTrigger.getMIDIMessages(mDefaultMIDIOutputChannel));
                        }
                        catch(ResolutionException re)
                        {
                            errors.add(new FileParseError(lineCounter,re.getMessage()));
                        }
                if(mSongFile.mSongSelectTrigger!=null)
                    if(mSongFile.mSongSelectTrigger.isSendable())
                        try {
                            initialMIDIMessages.addAll(mSongFile.mSongSelectTrigger.getMIDIMessages(mDefaultMIDIOutputChannel));
                        }
                        catch(ResolutionException re)
                        {
                            errors.add(new FileParseError(lineCounter,re.getMessage()));
                        }
            }

            // Now process all MIDI events with offsets.
            offsetMIDIEvents(firstEvent,errors);

            Song song=new Song(mSongFile,chosenAudioFile,chosenAudioVolume,comments,firstEvent,firstLine,errors,mUserChosenScrollMode,mSendMidiClock,mLoadingSongFile.getStartedByBandLeader(),mLoadingSongFile.getNextSong(),mLoadingSongFile.getSourceDisplaySettings().mOrientation,initialMIDIMessages,beatBlocks,initialBPB,count);
            song.doMeasurements(new Paint(),mCancelEvent,mSongLoadHandler,mLoadingSongFile.getNativeDisplaySettings(),mLoadingSongFile.getSourceDisplaySettings());
            return song;
        }
        finally
        {
            if(br!=null)
                try
                {
                    br.close();
                }
                catch(IOException ioe)
                {
                    Log.e(BeatPrompterApplication.TAG,"Failed to close song file.",ioe);
                }
        }
    }

    private static void offsetMIDIEvents(BaseEvent firstEvent,ArrayList<FileParseError> errors)
    {
        BaseEvent event=firstEvent;
        while(event!=null)
        {
            if(event instanceof MIDIEvent)
            {
                MIDIEvent midiEvent=(MIDIEvent)event;
                if((midiEvent.mOffset!=null)&&(midiEvent.mOffset.mAmount!=0))
                {
                    // OK, this event needs moved.
                    long newTime=-1;
                    if(midiEvent.mOffset.mOffsetType== EventOffset.OffsetType.Milliseconds)
                    {
                        long offset=Utils.milliToNano(midiEvent.mOffset.mAmount);
                        newTime=midiEvent.mEventTime+offset;
                    }
                    else
                    {
                        // Offset by beat count.
                        int beatCount=midiEvent.mOffset.mAmount;
                        BaseEvent currentEvent=midiEvent;
                        while(beatCount!=0)
                        {
                            BeatEvent beatEvent;
                            if(beatCount>0)
                                beatEvent=currentEvent.getNextBeatEvent();
                            else if((currentEvent instanceof BeatEvent)&&(currentEvent.mPrevEvent!=null))
                                beatEvent=currentEvent.mPrevEvent.mPrevBeatEvent;
                            else
                                beatEvent=currentEvent.mPrevBeatEvent;
                            if(beatEvent==null)
                                break;
                            if(beatEvent.mEventTime!=midiEvent.mEventTime) {
                                beatCount -= beatCount/Math.abs(beatCount);
                                newTime=beatEvent.mEventTime;
                            }
                            currentEvent=beatEvent;
                        }
                    }
                    if(newTime<0) {
                        errors.add(new FileParseError(midiEvent.mOffset.mSourceTag, BeatPrompterApplication.getResourceString(R.string.midi_offset_is_before_start_of_song)));
                        newTime=0;
                    }
                    MIDIEvent newMIDIEvent=new MIDIEvent(newTime,midiEvent.mMessages);
                    midiEvent.insertEvent(newMIDIEvent);
                    event=midiEvent.mPrevEvent;
                    midiEvent.remove();
                }
            }
            event=event.mNextEvent;
        }
    }

    private static long generatePause(long pauseTime,BaseEvent lastEvent,long currentTime)
    {
        // pauseTime is in milliseconds.
        // We don't want to generate thousands of events, so let's say every 1/10th of a second.
        int deciSeconds=(int)Math.ceil((double)pauseTime/100.0);
        long remainder=Utils.milliToNano(pauseTime)-Utils.milliToNano(deciSeconds*100);
        long oneDeciSecondInNanoseconds=Utils.milliToNano(100);
        currentTime+=remainder;
        for(int f=0;f<deciSeconds;++f)
        {
            PauseEvent pauseEvent= new PauseEvent(currentTime, deciSeconds, f);
            lastEvent.insertEvent(pauseEvent);
            lastEvent=lastEvent.getLastEvent();
            currentTime+=oneDeciSecondInNanoseconds;
        }
        return currentTime;
    }
}
