package com.stevenfrew.beatprompter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public class SongFile extends CachedFile
{
    private class SmoothScrollingTimes
    {
        SmoothScrollingTimes(long line,long bar,long track)
        {
            mTimePerLine=line;
            mTimePerBar=bar;
            mTrackLength=track;
        }
        long mTimePerLine=0;
        long mTimePerBar=0;
        long mTrackLength=0;
    }

    final static String SONGFILE_ELEMENT_TAG_NAME="song";

    private final static int DEMO_LINE_COUNT=15;

    private final static String SONG_FILE_TITLE_ATTRIBUTE_NAME="title";
    private final static String SONG_FILE_ARTIST_ATTRIBUTE_NAME="artist";
    private final static String SONG_FILE_LINECOUNT_ATTRIBUTE_NAME="lines";
    private final static String SONG_FILE_MIXED_MODE_ATTRIBUTE_NAME="mixedMode";
    private final static String SONG_FILE_BPM_ATTRIBUTE_NAME="bpm";
    private final static String SONG_FILE_KEY_ATTRIBUTE_NAME="key";
    private final static String SONG_FILE_TIME_PER_LINE_ATTRIBUTE_NAME="timePerLine";
    private final static String SONG_FILE_TIME_PER_BAR_ATTRIBUTE_NAME="timePerBar";
    private final static String TAG_ELEMENT_TAG_NAME="tag";
    private final static String AUDIO_FILE_ELEMENT_TAG_NAME="audio";
    private final static String IMAGE_FILE_ELEMENT_TAG_NAME="image";
    private final static String PROGRAM_CHANGE_TRIGGER_ELEMENT_TAG_NAME="programChangeTrigger";
    private final static String SONG_SELECT_TRIGGER_ELEMENT_TAG_NAME="songSelectTrigger";

    private long mTimePerLine=0;
    private long mTimePerBar=0;
    private double mBPM=0;
    String mKey="";
    int mLines=0;
    boolean mMixedMode=false;
    String mArtist;
    private Context mContext;

    private MIDISongTrigger mSongSelectTrigger=MIDISongTrigger.DEAD_TRIGGER;
    private MIDISongTrigger mProgramChangeTrigger=MIDISongTrigger.DEAD_TRIGGER;

    HashSet<String> mTags=new HashSet<>();
    ArrayList<String> mAudioFiles=new ArrayList<>();
    ArrayList<String> mImageFiles=new ArrayList<>();

    private final static int MAX_LINE_LENGTH=256;

    public SongFile(Context context,DownloadedFile downloadedFile, ArrayList<AudioFile> tempAudioFileCollection, ArrayList<ImageFile> tempImageFileCollection) throws IOException
    {
        super(downloadedFile);
        mContext=context;
        parseSongFileInfo(context,tempAudioFileCollection,tempImageFileCollection);
    }

    public SongFile(Context context,CachedFile cachedFile, String title, String artist, ArrayList<String> tags, ArrayList<String> audioTracks, ArrayList<String> imageFiles, double bpm, long timePerLine,long timePerBar,MIDISongTrigger programChangeTrigger,MIDISongTrigger songSelectTrigger, boolean mixedMode,int lines, String key) throws IOException    {
        super(cachedFile);
        mContext=context;
        mTags.addAll(tags);
        mLines=lines;
        mMixedMode=mixedMode;
        mAudioFiles=audioTracks;
        mImageFiles=imageFiles;
        mTitle=title;
        mArtist=artist;
        mKey=key;
        mBPM=bpm;
        mTimePerLine=timePerLine;
        mTimePerBar=timePerBar;
        mProgramChangeTrigger=programChangeTrigger;
        mSongSelectTrigger=songSelectTrigger;
    }

    private String getTitleFromLine(String line, int lineNumber)
    {
        return getTokenValue(line, lineNumber, "title", "t");
    }

    private String getKeyFromLine(String line, int lineNumber)
    {
        return getTokenValue(line, lineNumber, "key");
    }

    private String getFirstChordFromLine(String line, int lineNumber)
    {
        ArrayList<Tag> tagsOut=new ArrayList<>();
        Tag.extractTags(line,lineNumber,tagsOut);
        for(Tag t:tagsOut)
        {
            if(t.mChordTag)
                if(Utils.isChord(t.mName.trim()))
                    return t.mName.trim();
        }
        return null;
    }

    private String getArtistFromLine(String line, int lineNumber)
    {
        return getTokenValue(line, lineNumber, "artist", "a", "subtitle", "st");
    }

    private String getBPMFromLine(String line, int lineNumber)
    {
        return getTokenValue(line, lineNumber, "bpm", "beatsperminute", "metronome");
    }

    private ArrayList<String> getTagsFromLine(String line, int lineNumber)
    {
        return getTokenValues(line, lineNumber, "tag");
    }

    private MIDISongTrigger getMIDISongSelectTriggerFromLine(Context context,String line, int lineNumber)
    {
        return getMIDITriggerFromLine(context,line,lineNumber,true);
    }

    private MIDISongTrigger getMIDIProgramChangeTriggerFromLine(Context context,String line, int lineNumber)
    {
        return getMIDITriggerFromLine(context,line,lineNumber,false);
    }

    private MIDISongTrigger getMIDITriggerFromLine(Context context,String line,int lineNumber,boolean songSelectTrigger)
    {
        String val = getTokenValue(line, lineNumber, songSelectTrigger?"midi_song_select_trigger":"midi_program_change_trigger");
        if(val!=null)
            try
            {
                return MIDISongTrigger.parse(context,val,songSelectTrigger);
            }
            catch(Exception e)
            {
                Log.e(BeatPrompterApplication.TAG,"Failed to parse MIDI song trigger from song file.",e);
            }
        return null;
    }

    private ArrayList<String> getAudioFilesFromLine(String line, int lineNumber)
    {
        ArrayList<String> audio=new ArrayList<>();
        audio.addAll(getTokenValues(line, lineNumber, "audio"));
        audio.addAll(getTokenValues(line, lineNumber, "track"));
        audio.addAll(getTokenValues(line, lineNumber, "musicpath"));
        ArrayList<String> realaudio=new ArrayList<>();
        for(String str:audio)
        {
            int index=str.indexOf(":");
            if((index!=-1)&&(index<str.length()-1))
                str=str.substring(0,index);
            realaudio.add(str);
        }
        return realaudio;
    }

    private ArrayList<String> getImageFilesFromLine(String line, int lineNumber)
    {
        ArrayList<String> image=new ArrayList<>();
        image.addAll(getTokenValues(line, lineNumber, "image"));
        ArrayList<String> realimage=new ArrayList<>();
        for(String str:image)
        {
            int index=str.indexOf(":");
            if((index!=-1)&&(index<str.length()-1))
                str=str.substring(0,index);
            realimage.add(str);
        }
        return realimage;
    }

    private void parseSongFileInfo(Context context,ArrayList<AudioFile> tempAudioFileCollection,ArrayList<ImageFile> tempImageFileCollection) throws IOException
    {
        BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(mFile)));
        try
        {
            String line;
            int lineNumber=0;
            SmoothScrollingTimes sst = getTimePerLineAndBar(context,null,tempAudioFileCollection,tempImageFileCollection);
            mTimePerLine=sst.mTimePerLine;
            mTimePerBar=sst.mTimePerBar;
            while((line=br.readLine())!=null)
            {
                String title = getTitleFromLine(line, lineNumber);
                String artist=getArtistFromLine(line, lineNumber);
                String key=getKeyFromLine(line, lineNumber);
                String firstChord=getFirstChordFromLine(line,lineNumber);
                if(((mKey==null)||(mKey.length()==0))&&(firstChord!=null)&&(firstChord.length()>0))
                    mKey = firstChord;
                MIDISongTrigger msst=getMIDISongSelectTriggerFromLine(context,line,lineNumber);
                MIDISongTrigger mpct=getMIDIProgramChangeTriggerFromLine(context,line,lineNumber);
                if(msst!=null)
                    mSongSelectTrigger=msst;
                if(mpct!=null)
                    mProgramChangeTrigger=mpct;
                String bpm=getBPMFromLine(line, lineNumber);
                if(title!=null)
                    mTitle=title;
                if(key!=null)
                    mKey=key;
                if(artist!=null)
                    mArtist=artist;
                if((bpm!=null)&&(mBPM==0)) {
                    try {
                        mBPM = Double.parseDouble(bpm);
                    }
                    catch(Exception e)
                    {
                        Log.e(BeatPrompterApplication.TAG,"Failed to parse BPM value from song file.",e);
                    }
                }
                mMixedMode|=containsToken(line,lineNumber,"beatstart");
                ArrayList<String> tags=getTagsFromLine(line, lineNumber);
                mTags.addAll(tags);
                ArrayList<String> audio=getAudioFilesFromLine(line, lineNumber);
                mAudioFiles.addAll(audio);
                ArrayList<String> image=getImageFilesFromLine(line, lineNumber);
                mImageFiles.addAll(image);
                ++lineNumber;
            }
            mLines=lineNumber;
            if((mTitle==null)||(mTitle.length()==0))
                throw new InvalidBeatPrompterFileException(String.format(mContext.getString(R.string.noTitleFound), mStorageName));
            if(mArtist==null)
                mArtist="";
        }
        finally
        {
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

    boolean isSmoothScrollable()
    {
        return mTimePerLine>0;
    }

    boolean isBeatScrollable()
    {
        return mBPM>0;
    }

    static SongFile readFromXMLElement(Context context,Element element) throws IOException
    {
        CachedFile cf=CachedFile.readFromXMLElement(element);
        String title=element.getAttribute(SONG_FILE_TITLE_ATTRIBUTE_NAME);
        String artist=element.getAttribute(SONG_FILE_ARTIST_ATTRIBUTE_NAME);
        String bpmString=element.getAttribute(SONG_FILE_BPM_ATTRIBUTE_NAME);
        String lineString=element.getAttribute(SONG_FILE_LINECOUNT_ATTRIBUTE_NAME);
        String mixedModeString=element.getAttribute(SONG_FILE_MIXED_MODE_ATTRIBUTE_NAME);
        String keyString=element.getAttribute(SONG_FILE_KEY_ATTRIBUTE_NAME);
        if(keyString==null)
            keyString="";
        if(mixedModeString==null)
            mixedModeString="false";
        int lines=Integer.parseInt(lineString);
        double bpm=Double.parseDouble(bpmString);
        String timePerLineString=element.getAttribute(SONG_FILE_TIME_PER_LINE_ATTRIBUTE_NAME);
        boolean mixedMode=Boolean.parseBoolean(mixedModeString);
        long timePerLine=Long.parseLong(timePerLineString);
        String timePerBarString=element.getAttribute(SONG_FILE_TIME_PER_BAR_ATTRIBUTE_NAME);
        if((timePerBarString==null)||(timePerBarString.length()==0))
            timePerBarString="0";
        long timePerBar=Long.parseLong(timePerBarString);
        NodeList tagNodes=element.getElementsByTagName(TAG_ELEMENT_TAG_NAME);
        ArrayList<String> tags=new ArrayList<>();
        for(int f=0;f<tagNodes.getLength();++f)
            tags.add(tagNodes.item(f).getTextContent());
        NodeList audioNodes=element.getElementsByTagName(AUDIO_FILE_ELEMENT_TAG_NAME);
        ArrayList<String> audios=new ArrayList<>();
        for(int f=0;f<audioNodes.getLength();++f)
            audios.add(audioNodes.item(f).getTextContent());
        NodeList imageNodes=element.getElementsByTagName(IMAGE_FILE_ELEMENT_TAG_NAME);
        ArrayList<String> images=new ArrayList<>();
        for(int f=0;f<imageNodes.getLength();++f)
            images.add(imageNodes.item(f).getTextContent());
        NodeList pcTriggerNodes=element.getElementsByTagName(PROGRAM_CHANGE_TRIGGER_ELEMENT_TAG_NAME);
        MIDISongTrigger programChangeTrigger=MIDISongTrigger.DEAD_TRIGGER;
        for(int f=0;f<pcTriggerNodes.getLength();++f)
            programChangeTrigger=MIDISongTrigger.readFromXMLElement((Element)pcTriggerNodes.item(f));
        NodeList ssTriggerNodes=element.getElementsByTagName(SONG_SELECT_TRIGGER_ELEMENT_TAG_NAME);
        MIDISongTrigger songSelectTrigger=MIDISongTrigger.DEAD_TRIGGER;
        for(int f=0;f<ssTriggerNodes.getLength();++f)
            songSelectTrigger=MIDISongTrigger.readFromXMLElement((Element)ssTriggerNodes.item(f));
        return new SongFile(context,cf,title,artist,tags,audios,images,bpm,timePerLine,timePerBar,programChangeTrigger,songSelectTrigger,mixedMode,lines,keyString);
    }

    void writeToXML(Document doc, Element parent)
    {
        Element songElement = doc.createElement(SONGFILE_ELEMENT_TAG_NAME);
        super.writeToXML(songElement);
        songElement.setAttribute(SONG_FILE_TITLE_ATTRIBUTE_NAME, mTitle);
        songElement.setAttribute(SONG_FILE_ARTIST_ATTRIBUTE_NAME, mArtist);
        songElement.setAttribute(SONG_FILE_LINECOUNT_ATTRIBUTE_NAME, Integer.toString(mLines));
        songElement.setAttribute(SONG_FILE_MIXED_MODE_ATTRIBUTE_NAME, Boolean.toString(mMixedMode));
        songElement.setAttribute(SONG_FILE_BPM_ATTRIBUTE_NAME, Double.toString(mBPM));
        songElement.setAttribute(SONG_FILE_KEY_ATTRIBUTE_NAME, mKey);
        songElement.setAttribute(SONG_FILE_TIME_PER_LINE_ATTRIBUTE_NAME, Long.toString(mTimePerLine));
        songElement.setAttribute(SONG_FILE_TIME_PER_BAR_ATTRIBUTE_NAME, Long.toString(mTimePerBar));
        for (String tag : mTags)
        {
            Element tagElement=doc.createElement(TAG_ELEMENT_TAG_NAME);
            tagElement.setTextContent(tag);
            songElement.appendChild(tagElement);
        }
        for (String audioFile: mAudioFiles)
        {
            Element audioFileElement=doc.createElement(AUDIO_FILE_ELEMENT_TAG_NAME);
            audioFileElement.setTextContent(audioFile);
            songElement.appendChild(audioFileElement);
        }
        for (String imageFile: mImageFiles)
        {
            Element imageFileElement=doc.createElement(IMAGE_FILE_ELEMENT_TAG_NAME);
            imageFileElement.setTextContent(imageFile);
            songElement.appendChild(imageFileElement);
        }
        mProgramChangeTrigger.writeToXML(doc,songElement,PROGRAM_CHANGE_TRIGGER_ELEMENT_TAG_NAME);
        mSongSelectTrigger.writeToXML(doc,songElement,SONG_SELECT_TRIGGER_ELEMENT_TAG_NAME);
        parent.appendChild(songElement);
    }

    Song load(Context context, ScrollingMode userChosenScrollMode, String chosenTrack, boolean appRegistered, boolean startedByBandLeader, String nextSong, CancelEvent cancelEvent, Handler handler,boolean startedByMidiTrigger,ArrayList<MIDIAlias> aliases,SongDisplaySettings nativeSettings,SongDisplaySettings sourceSettings) throws IOException
    {
        // OK, the "scrollMode" param is passed in here.
        // This might be what the user has explicitly chosen, i.e.
        // smooth mode or manual mode, chosen via the long-press play dialog.
        ScrollingMode currentScrollMode=userChosenScrollMode;
        // BUT, if the mode that has come in is "beat mode", and this is a mixed mode
        // song, we should be switching when we encounter beatstart/beatstop tags.
        boolean allowModeSwitching=((mMixedMode)&&(userChosenScrollMode==ScrollingMode.Beat));
        if(allowModeSwitching)
            // And if we ARE in mixed mode with switching allowed, we start in manual.
            currentScrollMode=ScrollingMode.Manual;

        int countInOffset=Integer.parseInt(context.getString(R.string.pref_countIn_offset));
        int countInMin=Integer.parseInt(context.getString(R.string.pref_countIn_min))+countInOffset;
        int countInMax=Integer.parseInt(context.getString(R.string.pref_countIn_max))+countInOffset;
        int countInDefault=Integer.parseInt(context.getString(R.string.pref_countIn_default))+countInOffset;

        double bpmOffset=Integer.parseInt(context.getString(R.string.pref_bpm_offset));
        double bpmMin=Integer.parseInt(context.getString(R.string.pref_bpm_min))+bpmOffset;
        double bpmMax=Integer.parseInt(context.getString(R.string.pref_bpm_max))+bpmOffset;
        double bpmDefault=Integer.parseInt(context.getString(R.string.pref_bpm_default))+bpmOffset;

        int bplOffset=Integer.parseInt(context.getString(R.string.pref_bpl_offset));
        int bplMin=Integer.parseInt(context.getString(R.string.pref_bpl_min))+bplOffset;
        int bplMax=Integer.parseInt(context.getString(R.string.pref_bpl_max))+bplOffset;
        int bplDefault=Integer.parseInt(context.getString(R.string.pref_bpl_default))+bplOffset;

        int bpbOffset=Integer.parseInt(context.getString(R.string.pref_bpb_offset));
        int bpbMin=Integer.parseInt(context.getString(R.string.pref_bpb_min))+bpbOffset;
        int bpbMax=Integer.parseInt(context.getString(R.string.pref_bpb_max))+bpbOffset;
        int bpbDefault=Integer.parseInt(context.getString(R.string.pref_bpb_default))+bpbOffset;

        int scrollBeatMin=1;
        int scrollBeatDefault=4;

        ArrayList<MIDIOutgoingMessage> initialMIDIMessages=new ArrayList<>();
        ArrayList<FileParseError> errors=new ArrayList<>();
        boolean stopAddingStartupItems=false;

        SmoothScrollingTimes sst=getTimePerLineAndBar(context,chosenTrack,null,null);
        long timePerLine=sst.mTimePerLine;
        long timePerBar=sst.mTimePerBar;

        if((timePerLine<0)||(timePerBar<0)) {
            errors.add(new FileParseError(null, context.getString(R.string.pauseLongerThanSong)));
            sst.mTimePerLine=-timePerLine;
            sst.mTimePerBar=-timePerBar;
        }

        BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(mFile)));
        try
        {
            String line;

            ArrayList<Tag> tagsOut=new ArrayList<>();
            HashSet<String> tagsSet=new HashSet<>();

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            MIDITriggerOutputContext triggerContext=MIDITriggerOutputContext.valueOf(sharedPref.getString(context.getString(R.string.pref_sendMidiTriggerOnStart_key),context.getString(R.string.pref_sendMidiTriggerOnStart_defaultValue)));
            int countInPref = sharedPref.getInt(context.getString(R.string.pref_countIn_key), Integer.parseInt(context.getString(R.string.pref_countIn_default)));
            countInPref+=Integer.parseInt(context.getString(R.string.pref_countIn_offset));
/*            int defaultPausePref = sharedPref.getInt(context.getString(R.string.pref_defaultPause_key), Integer.parseInt(context.getString(R.string.pref_defaultPause_default)));
            defaultPausePref+=Integer.parseInt(context.getString(R.string.pref_defaultPause_offset));*/
            int defaultTrackVolume=sharedPref.getInt(context.getString(R.string.pref_defaultTrackVolume_key), Integer.parseInt(context.getString(R.string.pref_defaultTrackVolume_default)));
            defaultTrackVolume+=Integer.parseInt(context.getString(R.string.pref_defaultTrackVolume_offset));
            int defaultMIDIOutputChannelPrefValue=sharedPref.getInt(context.getString(R.string.pref_defaultMIDIOutputChannel_key),Integer.parseInt(context.getString(R.string.pref_defaultMIDIOutputChannel_default)));
            byte defaultMIDIOutputChannel=MIDIMessage.getChannelFromBitmask(defaultMIDIOutputChannelPrefValue);
            boolean showChords=sharedPref.getBoolean(context.getString(R.string.pref_showChords_key), Boolean.parseBoolean(context.getString(R.string.pref_showChords_defaultValue)));
            boolean sendMidiClock = sharedPref.getBoolean(context.getString(R.string.pref_sendMidi_key), false);
            int backgroundColour = sharedPref.getInt(context.getString(R.string.pref_backgroundColor_key), Color.parseColor(context.getString(R.string.pref_backgroundColor_default)));
            int pulseColour = sharedPref.getInt(context.getString(R.string.pref_pulseColor_key), Color.parseColor(context.getString(R.string.pref_pulseColor_default)));
            int beatCounterColour = sharedPref.getInt(context.getString(R.string.pref_beatCounterColor_key), Color.parseColor(context.getString(R.string.pref_beatCounterColor_default)));
            int scrollMarkerColour = sharedPref.getInt(context.getString(R.string.pref_scrollMarkerColor_key), Color.parseColor(context.getString(R.string.pref_scrollMarkerColor_default)));
            int lyricColour = sharedPref.getInt(context.getString(R.string.pref_lyricColor_key),Color.parseColor(context.getString(R.string.pref_lyricColor_default)));
            int chordColour = sharedPref.getInt(context.getString(R.string.pref_chordColor_key), Color.parseColor(context.getString(R.string.pref_chordColor_default)));
            int annotationColour = sharedPref.getInt(context.getString(R.string.pref_annotationColor_key), Color.parseColor(context.getString(R.string.pref_annotationColor_default)));
            String customCommentsUser=sharedPref.getString(context.getString(R.string.pref_customComments_key), context.getString(R.string.pref_customComments_defaultValue));
            backgroundColour|=0xff000000;
            annotationColour|=0xff000000;
            pulseColour|=0xff000000;
            beatCounterColour|=0xff000000;
            lyricColour|=0xff000000;
            chordColour|=0xff000000;
            boolean ignoreColorInfo=sharedPref.getBoolean(context.getString(R.string.pref_ignoreColorInfo_key), Boolean.parseBoolean(context.getString(R.string.pref_ignoreColorInfo_defaultValue)));
            String metronomePref=sharedPref.getString(context.getString(R.string.pref_metronome_key), context.getString(R.string.pref_metronome_defaultValue));

            // ONE SHOT
            String title=mTitle,artist=mArtist;
            AudioFile chosenAudioFile=null;
            int chosenAudioVolume=100;
            int count=countInPref;
            long trackOffset=0;
            // TIME
            double bpm=mBPM==0?120:mBPM;
            int bpb=4;
            int initialBPB=4;
            boolean initialBPBSet=false;
            String sKey="";
            int bpl=1;
            int beatsToAdjust=0;
            ArrayList<BeatEvent> rolloverBeats=new ArrayList<>();
            int pauseTime;
            int scrollBeat=bpb;
            MIDIBeatBlock lastMIDIBeatBlock=null;
            // COMMENT
            ArrayList<Comment> comments=new ArrayList<>();
            ArrayList<MIDIBeatBlock> beatBlocks=new ArrayList<>();
            String commentAudience;
            ImageFile lineImage=null;

            boolean metronomeOn=metronomePref.equals(context.getString(R.string.metronomeOnValue));
            boolean metronomeOnWhenNoBackingTrack=metronomePref.equals(context.getString(R.string.metronomeOnWhenNoBackingTrackValue));
            boolean metronomeCount=metronomePref.equals(context.getString(R.string.metronomeDuringCountValue));

            if(metronomeOnWhenNoBackingTrack && (chosenTrack==null || chosenTrack.length()==0))
                metronomeOn=true;

            int currentBeat=0;
            long nanosecondsPerBeat;

            ImageScalingMode imageScalingMode=ImageScalingMode.Stretch;
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
            handler.obtainMessage(BeatPrompterApplication.SONG_LOAD_LINE_PROCESSED,0,mLines).sendToTarget();
            while(((line=br.readLine())!=null)&&(!cancelEvent.isCancelled()))
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
                        errors.add(new FileParseError(null, String.format(context.getString(R.string.lineTooLong),lineCounter,MAX_LINE_LENGTH)));
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

                        if((Tag.COLOR_TAGS.contains(tag.mName))&&(!ignoreColorInfo))
                            createColorEvent=true;
                        if((Tag.ONE_SHOT_TAGS.contains(tag.mName))&&(tagsSet.contains(tag.mName)))
                            errors.add(new FileParseError(tag,String.format(context.getString(R.string.oneShotTagDefinedTwice),tag.mName)));
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
                            case "title":
                            case "t":
                                title=tag.mValue;
                                break;
                            case "artist":
                            case "a":
                            case "subtitle":
                            case "st":
                                artist=tag.mValue;
                                break;
                            case "image":
                                if(lineImage!=null)
                                {
                                    errors.add(new FileParseError(lineCounter, context.getString(R.string.multiple_images_in_one_line)));
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
                                        errors.add(new FileParseError(lineCounter,context.getString(R.string.unknown_image_scaling_mode)));
                                }
                                String image=new File(imageName).getName();
                                File imageFile=null;
                                ImageFile mappedImage=SongList.getMappedImageFilename(image,null);
                                if(mappedImage==null)
                                    errors.add(new FileParseError(tag, String.format(context.getString(R.string.cannotFindImageFile),image)));
                                else
                                {
                                    imageFile = new File(mFile.getParent(), mappedImage.mFile.getName());
                                    if (!imageFile.exists()) {
                                        errors.add(new FileParseError(tag, String.format(context.getString(R.string.cannotFindImageFile),image)));
                                        imageFile = null;
                                    }
                                }
                                lineImage=mappedImage;
                                break;
                            case "track":
                            case "audio":
                            case "musicpath":
                                String trackName=tag.mValue;
                                int volume=defaultTrackVolume;
                                int trackcolonindex=trackName.indexOf(":");
                                if((trackcolonindex!=-1)&&(trackcolonindex<trackName.length()-1))
                                {
                                    String strVolume=trackName.substring(trackcolonindex+1);
                                    trackName=trackName.substring(0,trackcolonindex);
                                    try
                                    {
                                        int tryvolume = Integer.parseInt(strVolume);
                                        if((tryvolume<0)||(tryvolume>100))
                                            errors.add(new FileParseError(lineCounter,context.getString(R.string.badAudioVolume)));
                                        else
                                            volume=(int)((double)volume*((double)tryvolume/100.0));
                                    }
                                    catch(NumberFormatException nfe)
                                    {
                                        errors.add(new FileParseError(lineCounter,context.getString(R.string.badAudioVolume)));
                                    }
                                }
                                String track=new File(trackName).getName();
                                File trackFile=null;
                                AudioFile mappedTrack=SongList.getMappedAudioFilename(track,null);
                                if(mappedTrack==null)
                                    errors.add(new FileParseError(tag, String.format(context.getString(R.string.cannotFindAudioFile),track)));
                                else
                                {
                                    trackFile = new File(mFile.getParent(), mappedTrack.mFile.getName());
                                    if (!trackFile.exists()) {
                                        errors.add(new FileParseError(tag, String.format(context.getString(R.string.cannotFindAudioFile),track)));
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
                                sendMidiClock=true;
                                break;
                            case "count":
                            case "countin":
                                count=Tag.getIntegerValueFromTag(mContext,tag, countInMin, countInMax, countInDefault, errors);
                                break;
//                            case "trackoffset":
//                                trackOffset=Tag.getLongValueFromTag(tag, trackOffsetMin, trackOffsetMax, trackOffsetDefault, errors);
//                                break;
                            case "backgroundcolour":
                            case "backgroundcolor":
                            case "bgcolour":
                            case "bgcolor":
                                backgroundColour=Tag.getColourValueFromTag(mContext,tag, backgroundColour, errors);
                                break;
                            case "pulsecolour":
                            case "pulsecolor":
                            case "beatcolour":
                            case "beatcolor":
                                pulseColour=Tag.getColourValueFromTag(mContext,tag, pulseColour, errors);
                                break;
                            case "lyriccolour":
                            case "lyriccolor":
                            case "lyricscolour":
                            case "lyricscolor":
                                lyricColour=Tag.getColourValueFromTag(mContext,tag, lyricColour, errors);
                                break;
                            case "chordcolour":
                            case "chordcolor":
                                chordColour=Tag.getColourValueFromTag(mContext,tag, chordColour, errors);
                                break;
                            case "beatcountercolour":
                            case "beatcountercolor":
                                beatCounterColour=Tag.getColourValueFromTag(mContext,tag, beatCounterColour, errors);
                                break;
                            case "bpm":
                            case "metronome":
                            case "beatsperminute":
                                bpm=Tag.getDoubleValueFromTag(mContext,tag, bpmMin, bpmMax, bpmDefault, errors);
                                break;
                            case "bpb":
                            case "beatsperbar":
                                int prevScrollBeatDiff=bpb-scrollBeat;
                                bpb=Tag.getIntegerValueFromTag(mContext,tag, bpbMin, bpbMax, bpbDefault, errors);
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
                                bpl=Tag.getIntegerValueFromTag(mContext,tag, bplMin, bplMax, bplDefault, errors);
                                break;
                            case "scrollbeat":
                            case "sb":
                                scrollBeat=Tag.getIntegerValueFromTag(mContext,tag, scrollBeatMin, bpb, scrollBeatDefault, errors);
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
                                    if (comment.isIntendedFor(customCommentsUser))
                                        comments.add(comment);
                                }
                                break;
                            case "pause":
                                pauseTime=Tag.getDurationValueFromTag(mContext,tag,1000,60*60*1000,0,false,errors);
                                break;
                            case "midi_song_select_trigger":
                            case "midi_program_change_trigger":
                                // Don't need the value after the song is loaded, we're just showing informational
                                // errors about bad formatting.
                                Tag.getSongTriggerFromTag(mContext,tag,errors);
                                break;
                            case "time":
                            case "tag":
                            case "bars":
                            case "soh":
                            case "eoh":
                            case "b":
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
                            case "key":
                                sKey=tag.mValue.trim();
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
                                    if((displayLineCounter>DEMO_LINE_COUNT)&&(!appRegistered))
                                        // NO MIDI FOR YOU
                                        break;
                                    MIDIEvent me = Tag.getMIDIEventFromTag(currentTime,mContext, tag, aliases, defaultMIDIOutputChannel, errors);
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
                                                errors.add(new FileParseError(tag,context.getString(R.string.midi_offset_before_first_line)));
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
                    // haven't set the key yet? Assume it's the first chord.
                    if((chordsFound)&&(sKey.length()==0))
                    {
                        for(Tag tag:tagsOut)
                            if(tag.mChordTag) {
                                if(Utils.isChord(tag.mName.trim())) {
                                    sKey = tag.mName.trim();
                                    break;
                                }
                            }
                    }
                    if((!showChords)&&(chordsFound))
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
                            ColorEvent styleEvent=new ColorEvent(currentTime,backgroundColour,pulseColour,lyricColour,chordColour,annotationColour,beatCounterColour,scrollMarkerColour);
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
                            errors.add(new FileParseError(lineCounter,context.getString(R.string.scrollbeatOffTheMap)));
                            scrollbeatOffset=0;
                        }
                        if(!commasFound)
                            bars=bpl;

                        if((lineImage!=null)&&((strippedLine.trim().length()>0)||(chordsFound)))
                            errors.add(new FileParseError(lineCounter,context.getString(R.string.text_found_with_image)));

                        if((strippedLine.trim().length()==0)&&(!chordsFound))
                            strippedLine="▼";

//                        if((firstLine==null)&&(smoothScrolling))
//                            pauseTime+=defaultPausePref*1000;

                        if(createLine)
                        {
                            displayLineCounter++;
                            if((displayLineCounter>DEMO_LINE_COUNT)&&(!appRegistered))
                            {
                                tagsOut=new ArrayList<>();
                                strippedLine = context.getString(R.string.please_buy);
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
                                lineObj = new ImageLine(lineImage, imageScalingMode, mContext, tagsOut, bars, lastEvent.mPrevColorEvent, bpbThisLine, scrollBeat, scrollbeatOffset,currentScrollMode, errors);
                                lineImage=null;
                            }
                            else
                                lineObj= new TextLine(strippedLine,mContext,tagsOut, bars, lastEvent.mPrevColorEvent, bpbThisLine, scrollBeat, scrollbeatOffset, currentScrollMode,errors);

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
                            if((totalLineTime==0)||(currentScrollMode==ScrollingMode.Smooth))
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
                            else if((bpm>0)&&(currentScrollMode!=ScrollingMode.Smooth))
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
                                    if((lastMIDIBeatBlock==null)||(nanoPerBeat!=lastMIDIBeatBlock.mNanoPerBeat)) {
                                        MIDIBeatBlock midiBeatBlock = lastMIDIBeatBlock = new MIDIBeatBlock(beatEvent.mEventTime, midiBeatCounter++, nanoPerBeat);
                                        beatBlocks.add(midiBeatBlock);
                                    }

                                    if (currentBarBeat == beatsForThisLine - 1) {
                                        lineEvent.mLine.mYStartScrollTime = currentScrollMode==ScrollingMode.Smooth?lineEvent.mEventTime:currentTime;
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
                handler.obtainMessage(BeatPrompterApplication.SONG_LOAD_LINE_READ,lineCounter,mLines).sendToTarget();
            }

            long countTime = 0;
            // Create count events
            if((bpm>0)&&(currentScrollMode!=ScrollingMode.Manual))
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
                            BeatEvent countEvent = new BeatEvent(countTime,countbpm, countbpb, countbpl, g, metronomeCount || metronomeOn,(f==count-1?bpb-1:-1));
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
            if((bpm>0)&&(currentScrollMode==ScrollingMode.Beat))
            {
                // Last Y scroll should never happen. No point scrolling last line offscreen.
                Line mLastLine = firstLine.getLastLine();
                if (mLastLine != null)
                    mLastLine.mYStartScrollTime = mLastLine.mYStopScrollTime = Long.MAX_VALUE;
            }

            // Nothing at all in the song file? We at least want the colors set right.
            if(firstEvent==null)
                firstEvent=new ColorEvent(currentTime,backgroundColour,pulseColour,lyricColour,chordColour,annotationColour,beatCounterColour,scrollMarkerColour);

            BaseEvent reallyTheLastEvent=firstEvent.getLastEvent();
            // In beat mode, or in any other mode where we're using a backing track, let's have an end event.
            if((trackEvent!=null)||(currentScrollMode!=ScrollingMode.Manual))
            {
                long trackEndTime=0;
                if(trackEvent!=null)
                    trackEndTime=trackEvent.mEventTime+sst.mTrackLength;
                // The end event will be where the final beat occurs.
                // But there is a chance that the audio track is longer than that.
                EndEvent endEvent = new EndEvent(Math.max(currentTime,trackEndTime));
                reallyTheLastEvent.add(endEvent);
            }

            if((triggerContext==MIDITriggerOutputContext.Always)||(triggerContext==MIDITriggerOutputContext.ManualStartOnly && !startedByMidiTrigger))
            {
                if(mProgramChangeTrigger!=null)
                    if(mProgramChangeTrigger.isSendable())
                        initialMIDIMessages.addAll(mProgramChangeTrigger.getMIDIMessages(defaultMIDIOutputChannel));
                if(mSongSelectTrigger!=null)
                    if(mSongSelectTrigger.isSendable())
                        initialMIDIMessages.addAll(mSongSelectTrigger.getMIDIMessages(defaultMIDIOutputChannel));
            }

            // Now process all MIDI events with offsets.
            offsetMIDIEvents(context,firstEvent,errors);

            Song song=new Song(title,artist,chosenAudioFile,chosenAudioVolume,comments,firstEvent,firstLine,errors,userChosenScrollMode,sendMidiClock,startedByBandLeader,nextSong,sourceSettings.mOrientation,initialMIDIMessages,beatBlocks,mKey,mBPM,initialBPB,count);
            song.doMeasurements(context,new Paint(),cancelEvent,handler,nativeSettings,sourceSettings);
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

    private void offsetMIDIEvents(Context context,BaseEvent firstEvent,ArrayList<FileParseError> errors)
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
                    if(midiEvent.mOffset.mOffsetType== MIDIEventOffset.OffsetType.Milliseconds)
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
                        errors.add(new FileParseError(midiEvent.mOffset.mSourceTag, context.getString(R.string.midi_offset_is_before_start_of_song)));
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

    private long generatePause(long pauseTime,BaseEvent lastEvent,long currentTime)
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

    private SmoothScrollingTimes getTimePerLineAndBar(Context context, String chosenTrack, ArrayList<AudioFile> tempAudioFileCollection, ArrayList<ImageFile> tempImageFileCollection) throws IOException
    {
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
/*        int defaultPausePref = sharedPref.getInt(context.getString(R.string.pref_defaultPause_key), Integer.parseInt(context.getString(R.string.pref_defaultPause_default)));
        defaultPausePref+=Integer.parseInt(context.getString(R.string.pref_defaultPause_offset));*/
        BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(mFile)));

        int bplOffset=Integer.parseInt(context.getString(R.string.pref_bpl_offset));
        int bplMin=Integer.parseInt(context.getString(R.string.pref_bpl_min))+bplOffset;
        int bplMax=Integer.parseInt(context.getString(R.string.pref_bpl_max))+bplOffset;
        int bplDefault=Integer.parseInt(context.getString(R.string.pref_bpl_default))+bplOffset;

        try
        {
            long songTime=0;
            int songMilli=0;
            long pauseTime=0;
            int realLineCount=0;
            int realBarCount=0;
            int barsPerLine=bplDefault;
            String sKey="";
            long totalPauseTime=0;//defaultPausePref*1000;
            String line;
            ImageFile lineImage=null;
            int lineNumber=0;
            ArrayList<FileParseError> errors=new ArrayList<>();
            ArrayList<Tag> tagsOut=new ArrayList<>();
            while((line=br.readLine())!=null)
            {
                line=line.trim();
                lineNumber++;
                // Ignore comments.
                if(!line.startsWith("#"))
                {
                    tagsOut.clear();
                    String strippedLine=Tag.extractTags(line, lineNumber, tagsOut);
                    // Replace stupid unicode BOM character
                    strippedLine = strippedLine.replace("\uFEFF", "");
                    boolean chordsFound=false;
                    int barsTag=0;
                    for(Tag tag:tagsOut) {
                        // Not bothered about chords at the moment.
                        if (tag.mChordTag) {
                            if(sKey.length()==0)
                                if(Utils.isChord(tag.mName.trim()))
                                    sKey=tag.mName.trim();
                            chordsFound = true;
                            continue;
                        }

                        switch (tag.mName)
                        {
                            case "key":
                                sKey=tag.mValue.trim();
                                break;
                            case "time":
                                songTime = Tag.getDurationValueFromTag(mContext,tag, 1000, 60 * 60 * 1000, 0, true,errors);
                                break;
                            case "pause":
                                pauseTime = Tag.getDurationValueFromTag(mContext, tag, 1000, 60 * 60 * 1000, 0, false,errors);
                                break;
                            case "bars":
                            case "b":
                                barsTag=Tag.getIntegerValueFromTag(context, tag, 1, 128, 1, errors);
                                break;
                            case "bpl":
                            case "barsperline":
                                barsPerLine=Tag.getIntegerValueFromTag(mContext,tag, bplMin, bplMax, bplDefault, errors);
                                break;
                            case "image":
                                if(lineImage!=null)
                                {
                                    errors.add(new FileParseError(tag, context.getString(R.string.multiple_images_in_one_line)));
                                    break;
                                }
                                String imageName=tag.mValue;
                                int colonindex=imageName.indexOf(":");
                                if((colonindex!=-1)&&(colonindex<imageName.length()-1))
                                    imageName=imageName.substring(0,colonindex);
                                String image=new File(imageName).getName();
                                File imageFile=null;
                                ImageFile mappedImage=SongList.getMappedImageFilename(image,tempImageFileCollection);
                                if(mappedImage==null)
                                    errors.add(new FileParseError(tag, String.format(context.getString(R.string.cannotFindImageFile),image)));
                                else
                                {
                                    imageFile = new File(mFile.getParent(), mappedImage.mFile.getName());
                                    if (!imageFile.exists()) {
                                        errors.add(new FileParseError(tag, String.format(context.getString(R.string.cannotFindImageFile),image)));
                                        mappedImage=null;
                                    }
                                }
                                lineImage=mappedImage;
                                break;
                            case "track":
                            case "audio":
                            case "musicpath":
                                String trackName=tag.mValue;
                                int trackColonindex=trackName.indexOf(":");
                                // volume?
                                if((trackColonindex!=-1)&&(trackColonindex<trackName.length()-1))
                                    trackName=trackName.substring(0,trackColonindex);
                                String track=new File(trackName).getName();
                                File trackFile=null;
                                AudioFile mappedTrack=SongList.getMappedAudioFilename(track,tempAudioFileCollection);
                                if(mappedTrack==null) {
                                    errors.add(new FileParseError(tag, String.format(context.getString(R.string.cannotFindAudioFile), track)));
                                }
                                else
                                {
                                    trackFile = new File(mFile.getParent(), mappedTrack.mFile.getName());
                                    if (!trackFile.exists()) {
                                        errors.add(new FileParseError(tag, String.format(context.getString(R.string.cannotFindAudioFile),track)));
                                        trackFile = null;
                                    }
                                }
                                if((songMilli==0)&&(trackFile!=null)&&((chosenTrack==null)||(track.equalsIgnoreCase(chosenTrack))))
                                {
                                    try
                                    {
                                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                                        mmr.setDataSource(trackFile.getAbsolutePath());
                                        String data = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                                        if(data!=null)
                                            songMilli=Integer.parseInt(data);
                                    }
                                    catch(Exception e)
                                    {
                                        Log.e(BeatPrompterApplication.TAG,"Failed to extract duration metadata from media file.",e);
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                    }

                    int bars=barsTag;
                    if(bars==0) {
                        boolean commasFound = false;
                        while (strippedLine.startsWith(",")) {
                            commasFound = true;
                            strippedLine = strippedLine.substring(1);
                            bars++;
                        }
                        bars = Math.max(1, bars);
                        if (!commasFound)
                            bars = barsPerLine;
                    }

                    // Contains only tags? Or contains nothing? Don't use it as a blank line.
                    if((strippedLine.trim().length()>0)||(chordsFound)||(pauseTime>0)||(lineImage!=null))
                    {
                        lineImage=null;
                        totalPauseTime += pauseTime;
                        pauseTime=0;
                        // Line could potentially have been "{sometag} # comment"?
                        if ((lineImage!=null)||(chordsFound)||(!strippedLine.trim().startsWith("#")))
                        {
                            realBarCount+=bars;
                            realLineCount++;
                        }
                    }
                }
                ++lineNumber;
            }

            if(songTime==Utils.TRACK_AUDIO_LENGTH_VALUE)
                songTime=songMilli;

            boolean negateResult=false;
            if(totalPauseTime>songTime) {
                negateResult = true;
                totalPauseTime=0;
            }
            long lineresult=(long)((double)(Utils.milliToNano(songTime - totalPauseTime))/(double)realLineCount);
            long barresult=(long)((double)(Utils.milliToNano(songTime - totalPauseTime))/(double)realBarCount);
            long trackresult=Utils.milliToNano(songMilli);
            if(negateResult) {
                lineresult = -lineresult;
                barresult=-barresult;
            }
            return new SmoothScrollingTimes(lineresult,barresult,trackresult);
        }
        finally
        {
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

    boolean matchesTrigger(MIDISongTrigger trigger)
    {
        return ((mSongSelectTrigger!=null && mSongSelectTrigger.equals(trigger))
            ||(mProgramChangeTrigger!=null && mProgramChangeTrigger.equals(trigger)));
    }

    @Override
    CachedFileType getFileType()
    {
        return CachedFileType.Song;
    }

}
