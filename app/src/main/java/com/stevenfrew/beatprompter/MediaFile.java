package com.stevenfrew.beatprompter;

import org.w3c.dom.Element;

import java.io.File;

import java.util.Date;

abstract class MediaFile extends CachedCloudFile
{
    private final static String MEDIAFILE_TITLE_ATTRIBUTE_NAME="title";

    String mTitle;

    MediaFile(File file,String storageID,String title,Date lastModified,String subfolder)
    {
        super(file,storageID,title,lastModified,subfolder);
        mTitle=title;
    }

    MediaFile(Element e)
    {
        super(e);
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
