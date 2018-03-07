package com.stevenfrew.beatprompter;

import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import java.util.Date;

class AudioFile extends MediaFile
{
    final static String AUDIOFILE_ELEMENT_TAG_NAME="audiofile";

    AudioFile(String title,File file,String storageName,Date lastModified)
    {
        super(title,file,storageName,lastModified);
        mTitle=title;
    }

    private AudioFile(CachedFile cf, String title)
    {
        super(cf,title);
    }

    void writeToXML(Document doc, Element parent)
    {
        Element audioFileElement = doc.createElement(AUDIOFILE_ELEMENT_TAG_NAME);
        super.writeToXML(audioFileElement);
        parent.appendChild(audioFileElement);
    }

    static AudioFile readFromXMLElement(Element element)
    {
        CachedFile cf=CachedFile.readFromXMLElement(element);
        String title=MediaFile.readMediaTitle(element);
        return new AudioFile(cf,title);
    }
}
