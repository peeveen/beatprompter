package com.stevenfrew.beatprompter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import java.util.Date;

abstract class MediaFile extends CachedFile
{
    private final static String MEDIAFILE_TITLE_ATTRIBUTE_NAME="title";

    String mTitle;

    MediaFile(String title,File file,String storageName,Date lastModified)
    {
        super(file,storageName,lastModified,"");
        mTitle=title;
    }

    protected MediaFile(CachedFile cf, String title)
    {
        super(cf);
        mTitle=title;
    }

    void writeToXML(Element mediaFileElement)
    {
        super.writeToXML(mediaFileElement);
        mediaFileElement.setAttribute(MEDIAFILE_TITLE_ATTRIBUTE_NAME, mTitle);
    }

    static String readMediaTitle(Element element)
    {
        return element.getAttribute(MEDIAFILE_TITLE_ATTRIBUTE_NAME);
    }
}
