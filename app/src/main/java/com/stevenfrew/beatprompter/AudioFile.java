package com.stevenfrew.beatprompter;

import android.media.MediaMetadataRetriever;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import java.util.Date;

class AudioFile extends CachedCloudFile
{
    final static String AUDIOFILE_ELEMENT_TAG_NAME="audiofile";

    AudioFile(CloudDownloadResult result) throws InvalidBeatPrompterFileException
    {
        super(result);
        verifyAudioFile();
    }

    AudioFile(File file,String storageID,String name,Date lastModified,String subfolder) throws InvalidBeatPrompterFileException
    {
        super(file,storageID,name,lastModified,subfolder);
        verifyAudioFile();
    }

    AudioFile(Element e)
    {
        super(e);
    }

    void writeToXML(Document doc, Element parent)
    {
        Element audioFileElement = doc.createElement(AUDIOFILE_ELEMENT_TAG_NAME);
        super.writeToXML(audioFileElement);
        parent.appendChild(audioFileElement);
    }

    @Override
    CloudFileType getFileType()
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
