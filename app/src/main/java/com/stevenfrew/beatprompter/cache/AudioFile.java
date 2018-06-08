package com.stevenfrew.beatprompter.cache;

import android.media.MediaMetadataRetriever;

import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;
import com.stevenfrew.beatprompter.cloud.CloudFileType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import java.util.Date;

public class AudioFile extends CachedCloudFile
{
    public final static String AUDIOFILE_ELEMENT_TAG_NAME="audiofile";

    AudioFile(CloudDownloadResult result) throws InvalidBeatPrompterFileException
    {
        super(result);
        verifyAudioFile();
    }

    public AudioFile(File file,String storageID,String name,Date lastModified,String subfolder) throws InvalidBeatPrompterFileException
    {
        super(file,storageID,name,lastModified,subfolder);
        verifyAudioFile();
    }

    public AudioFile(Element e)
    {
        super(e);
    }

    public void writeToXML(Document doc, Element parent)
    {
        Element audioFileElement = doc.createElement(AUDIOFILE_ELEMENT_TAG_NAME);
        super.writeToXML(audioFileElement);
        parent.appendChild(audioFileElement);
    }

    @Override
    public CloudFileType getFileType()
    {
        return CloudFileType.Audio;
    }

    private void verifyAudioFile() throws InvalidBeatPrompterFileException
    {
        try
        {
            // Try to read the length of the track. If it fails, it's not an audio file.
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(mFile.getAbsolutePath());
            mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        }
        catch(Exception e)
        {
            throw new InvalidBeatPrompterFileException(String.format(SongList.getContext().getString(R.string.notAnAudioFile), mName));
        }
    }
}
