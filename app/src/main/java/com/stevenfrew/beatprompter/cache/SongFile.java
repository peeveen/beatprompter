package com.stevenfrew.beatprompter.cache;

import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.stevenfrew.beatprompter.SmoothScrollingTimings;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.Utils;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;
import com.stevenfrew.beatprompter.midi.SongTrigger;

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

public class SongFile extends CachedCloudFile
{
    private final static String SONG_FILE_TITLE_ATTRIBUTE_NAME="title";
    private final static String SONG_FILE_ARTIST_ATTRIBUTE_NAME="artist";
    private final static String SONG_FILE_LINECOUNT_ATTRIBUTE_NAME="lines";
    private final static String SONG_FILE_MIXED_MODE_ATTRIBUTE_NAME="mixedMode";
    private final static String SONG_FILE_BPM_ATTRIBUTE_NAME="bpm";
    private final static String SONG_FILE_KEY_ATTRIBUTE_NAME="key";
    private final static String SONG_FILE_TIME_PER_LINE_ATTRIBUTE_NAME="timePerLine";
    private final static String SONG_FILE_TIME_PER_BAR_ATTRIBUTE_NAME="timePerBar";
    private final static String TAG_ELEMENT_TAG_NAME="tag";

    public final static String SONGFILE_ELEMENT_TAG_NAME="song";
    private final static String AUDIO_FILE_ELEMENT_TAG_NAME="audio";
    private final static String IMAGE_FILE_ELEMENT_TAG_NAME="image";

    private final static String PROGRAM_CHANGE_TRIGGER_ELEMENT_TAG_NAME="programChangeTrigger";
    private final static String SONG_SELECT_TRIGGER_ELEMENT_TAG_NAME="songSelectTrigger";

    private long mTimePerLine=0;
    private long mTimePerBar=0;
    public double mBPM=0;
    public String mTitle=null;
    public String mKey="";
    public int mLines=0;
    public boolean mMixedMode=false;
    public String mArtist;

    public SongTrigger mSongSelectTrigger= SongTrigger.DEAD_TRIGGER;
    public SongTrigger mProgramChangeTrigger= SongTrigger.DEAD_TRIGGER;

    public HashSet<String> mTags=new HashSet<>();
    public ArrayList<String> mAudioFiles=new ArrayList<>();
    public ArrayList<String> mImageFiles=new ArrayList<>();

    public SongFile(CloudDownloadResult result) throws InvalidBeatPrompterFileException
    {
        super(result.mDownloadedFile,result.mCloudFileInfo);
        parseSongFileInfo(new ArrayList<>(),new ArrayList<>());
    }

    public SongFile(File file,String id,String name,Date lastModified,String subfolder, ArrayList<AudioFile> tempAudioFileCollection, ArrayList<ImageFile> tempImageFileCollection) throws IOException
    {
        super(file,id,name,lastModified,subfolder);
        parseSongFileInfo(tempAudioFileCollection,tempImageFileCollection);
    }

    public SongFile(Element element)
    {
        super(element);
        mTitle=element.getAttribute(SONG_FILE_TITLE_ATTRIBUTE_NAME);
        mArtist=element.getAttribute(SONG_FILE_ARTIST_ATTRIBUTE_NAME);
        String bpmString=element.getAttribute(SONG_FILE_BPM_ATTRIBUTE_NAME);
        String lineString=element.getAttribute(SONG_FILE_LINECOUNT_ATTRIBUTE_NAME);
        String mixedModeString=element.getAttribute(SONG_FILE_MIXED_MODE_ATTRIBUTE_NAME);
        String keyString=element.getAttribute(SONG_FILE_KEY_ATTRIBUTE_NAME);
        if(keyString!=null)
            mKey=keyString;
        if(mixedModeString==null)
            mixedModeString="false";
        mLines=Integer.parseInt(lineString);
        mBPM=Double.parseDouble(bpmString);
        String timePerLineString=element.getAttribute(SONG_FILE_TIME_PER_LINE_ATTRIBUTE_NAME);
        mMixedMode=Boolean.parseBoolean(mixedModeString);
        mTimePerLine=Long.parseLong(timePerLineString);
        String timePerBarString=element.getAttribute(SONG_FILE_TIME_PER_BAR_ATTRIBUTE_NAME);
        if((timePerBarString==null)||(timePerBarString.length()==0))
            timePerBarString="0";
        mTimePerBar=Long.parseLong(timePerBarString);
        NodeList tagNodes=element.getElementsByTagName(TAG_ELEMENT_TAG_NAME);
        mTags=new HashSet<>();
        for(int f=0;f<tagNodes.getLength();++f)
            mTags.add(tagNodes.item(f).getTextContent());
        NodeList audioNodes=element.getElementsByTagName(AUDIO_FILE_ELEMENT_TAG_NAME);
        mAudioFiles=new ArrayList<>();
        for(int f=0;f<audioNodes.getLength();++f)
            mAudioFiles.add(audioNodes.item(f).getTextContent());
        NodeList imageNodes=element.getElementsByTagName(IMAGE_FILE_ELEMENT_TAG_NAME);
        mImageFiles=new ArrayList<>();
        for(int f=0;f<imageNodes.getLength();++f)
            mImageFiles.add(imageNodes.item(f).getTextContent());
        NodeList pcTriggerNodes=element.getElementsByTagName(PROGRAM_CHANGE_TRIGGER_ELEMENT_TAG_NAME);
        mProgramChangeTrigger= SongTrigger.DEAD_TRIGGER;
        for(int f=0;f<pcTriggerNodes.getLength();++f)
            mProgramChangeTrigger= SongTrigger.readFromXMLElement((Element)pcTriggerNodes.item(f));
        NodeList ssTriggerNodes=element.getElementsByTagName(SONG_SELECT_TRIGGER_ELEMENT_TAG_NAME);
        mSongSelectTrigger= SongTrigger.DEAD_TRIGGER;
        for(int f=0;f<ssTriggerNodes.getLength();++f)
            mSongSelectTrigger= SongTrigger.readFromXMLElement((Element)ssTriggerNodes.item(f));
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

    private SongTrigger getMIDISongSelectTriggerFromLine(String line, int lineNumber)
    {
        return getMIDITriggerFromLine(line,lineNumber,true);
    }

    private SongTrigger getMIDIProgramChangeTriggerFromLine(String line, int lineNumber)
    {
        return getMIDITriggerFromLine(line,lineNumber,false);
    }

    private SongTrigger getMIDITriggerFromLine(String line, int lineNumber, boolean songSelectTrigger)
    {
        String val = getTokenValue(line, lineNumber, songSelectTrigger?"midi_song_select_trigger":"midi_program_change_trigger");
        if(val!=null)
            try
            {
                return SongTrigger.parse(val,songSelectTrigger,lineNumber,new ArrayList<>());
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
        ArrayList<String> image = new ArrayList<>(getTokenValues(line, lineNumber, "image"));
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

    private void parseSongFileInfo(ArrayList<AudioFile> tempAudioFileCollection,ArrayList<ImageFile> tempImageFileCollection) throws InvalidBeatPrompterFileException
    {
        BufferedReader br=null;
        try
        {
            SmoothScrollingTimings sst = getTimePerLineAndBar(null,tempAudioFileCollection,tempImageFileCollection);
            mTimePerLine=sst.getTimePerLine();
            mTimePerBar=sst.getTimePerBar();
            br=new BufferedReader(new InputStreamReader(new FileInputStream(mFile)));
            String line;
            int lineNumber=0;
            while((line=br.readLine())!=null)
            {
                String title = getTitleFromLine(line, lineNumber);
                String artist=getArtistFromLine(line, lineNumber);
                String key=getKeyFromLine(line, lineNumber);
                String firstChord=getFirstChordFromLine(line,lineNumber);
                if(((mKey==null)||(mKey.length()==0))&&(firstChord!=null)&&(firstChord.length()>0))
                    mKey = firstChord;
                SongTrigger msst=getMIDISongSelectTriggerFromLine(line,lineNumber);
                SongTrigger mpct=getMIDIProgramChangeTriggerFromLine(line,lineNumber);
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
                throw new InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.noTitleFound, mName));
            if(mArtist==null)
                mArtist="";
        }
        catch(IOException ioe)
        {
            throw new InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.file_io_read_error),ioe);
        }
        finally
        {
            try
            {
                if(br!=null)
                    br.close();
            }
            catch(IOException ioe)
            {
                Log.e(BeatPrompterApplication.TAG,"Failed to close song file.",ioe);
            }
        }
    }

    public boolean isSmoothScrollable()
    {
        return mTimePerLine>0;
    }

    public boolean isBeatScrollable()
    {
        return mBPM>0;
    }

    public void writeToXML(Document doc, Element parent)
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

    public boolean matchesTrigger(SongTrigger trigger)
    {
        return ((mSongSelectTrigger!=null && mSongSelectTrigger.equals(trigger))
            ||(mProgramChangeTrigger!=null && mProgramChangeTrigger.equals(trigger)));
    }

    public SmoothScrollingTimings getTimePerLineAndBar(String chosenTrack, ArrayList<AudioFile> tempAudioFileCollection, ArrayList<ImageFile> tempImageFileCollection) throws IOException
    {
        int bplOffset=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_offset));
        int bplMin=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_min))+bplOffset;
        int bplMax=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_max))+bplOffset;
        int bplDefault=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_bpl_default))+bplOffset;

//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
/*        int defaultPausePref = sharedPref.getInt(context.getString(R.string.pref_defaultPause_key), Integer.parseInt(context.getString(R.string.pref_defaultPause_default)));
        defaultPausePref+=Integer.parseInt(context.getString(R.string.pref_defaultPause_offset));*/
        BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(mFile)));

        try
        {
            long songTime=0;
            int songMilli=0;
            long pauseTime=0;
            int realLineCount=0;
            int realBarCount=0;
            int barsPerLine=bplDefault;
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
                            chordsFound = true;
                            continue;
                        }

                        switch (tag.mName)
                        {
                            case "time":
                                songTime = Tag.getDurationValueFromTag(tag, 1000, 60 * 60 * 1000, 0, true,errors);
                                break;
                            case "pause":
                                pauseTime = Tag.getDurationValueFromTag( tag, 1000, 60 * 60 * 1000, 0, false,errors);
                                break;
                            case "bars":
                            case "b":
                                barsTag=Tag.getIntegerValueFromTag(tag, 1, 128, 1, errors);
                                break;
                            case "bpl":
                            case "barsperline":
                                barsPerLine=Tag.getIntegerValueFromTag(tag, bplMin, bplMax, bplDefault, errors);
                                break;
                            case "image":
                                if(lineImage!=null)
                                {
                                    errors.add(new FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.multiple_images_in_one_line)));
                                    break;
                                }
                                String imageName=tag.mValue;
                                int colonindex=imageName.indexOf(":");
                                if((colonindex!=-1)&&(colonindex<imageName.length()-1))
                                    imageName=imageName.substring(0,colonindex);
                                String image=new File(imageName).getName();
                                File imageFile;
                                ImageFile mappedImage= SongList.mCachedCloudFiles.getMappedImageFilename(image,tempImageFileCollection);
                                if(mappedImage==null)
                                    errors.add(new FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindImageFile,image)));
                                else
                                {
                                    imageFile = new File(mFile.getParent(), mappedImage.mFile.getName());
                                    if (!imageFile.exists()) {
                                        errors.add(new FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindImageFile,image)));
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
                                AudioFile mappedTrack=SongList.mCachedCloudFiles.getMappedAudioFilename(track,tempAudioFileCollection);
                                if(mappedTrack==null) {
                                    errors.add(new FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindAudioFile, track)));
                                }
                                else
                                {
                                    trackFile = new File(mFile.getParent(), mappedTrack.mFile.getName());
                                    if (!trackFile.exists()) {
                                        errors.add(new FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.cannotFindAudioFile,track)));
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
            return new SmoothScrollingTimings(lineresult,barresult,trackresult);
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
}
