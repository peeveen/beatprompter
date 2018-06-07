package com.stevenfrew.beatprompter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import java.util.Date;

class AudioFile extends MediaFile
{
    final static String AUDIOFILE_ELEMENT_TAG_NAME="audiofile";

    AudioFile(String title,File file,String storageID,Date lastModified,String subfolder)
    {
        super(file,storageID,title,lastModified,subfolder);
        mTitle=title;
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
}
