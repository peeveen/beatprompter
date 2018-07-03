package com.stevenfrew.beatprompter.cache;

import android.media.MediaMetadataRetriever;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AudioFile extends CachedCloudFile
{
    public final static String AUDIOFILE_ELEMENT_TAG_NAME="audiofile";

    AudioFile(CloudDownloadResult result) throws InvalidBeatPrompterFileException
    {
        super(result);
        verifyAudioFile();
    }

    AudioFile(Element e)
    {
        super(e);
    }

    public void writeToXML(Document doc, Element parent)
    {
        Element audioFileElement = doc.createElement(AUDIOFILE_ELEMENT_TAG_NAME);
        super.writeToXML(audioFileElement);
        parent.appendChild(audioFileElement);
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
            throw new InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.notAnAudioFile, mName));
        }
    }
}
