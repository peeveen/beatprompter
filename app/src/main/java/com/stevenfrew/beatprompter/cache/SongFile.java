package com.stevenfrew.beatprompter.cache;

import android.util.Log;

import com.stevenfrew.beatprompter.ScrollingMode;
import com.stevenfrew.beatprompter.SmoothScrollingTimings;
import com.stevenfrew.beatprompter.SongLoader;
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
            br=new BufferedReader(new InputStreamReader(new FileInputStream(mFile)));
            String line;
            int lineNumber=0;
            SongLoader loader=new SongLoader(this, ScrollingMode.Beat); // scroll mode not important
            SmoothScrollingTimings sst = loader.getTimePerLineAndBar(null,tempAudioFileCollection,tempImageFileCollection);
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

}
