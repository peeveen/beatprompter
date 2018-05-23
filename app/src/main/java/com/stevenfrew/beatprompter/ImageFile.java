package com.stevenfrew.beatprompter;

import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import java.util.Date;

class ImageFile extends MediaFile
{
    final static String IMAGEFILE_ELEMENT_TAG_NAME="imagefile";

    ImageFile(String title,File file,String storageName,Date lastModified)
    {
        super(title,file,storageName,lastModified);
    }

    private ImageFile(CachedFile cf, String title)
    {
        super(cf,title);
    }

    void writeToXML(Document doc, Element parent)
    {
        Element imageFileElement = doc.createElement(IMAGEFILE_ELEMENT_TAG_NAME);
        super.writeToXML(imageFileElement);
        parent.appendChild(imageFileElement);
    }

    static ImageFile readFromXMLElement(Element element)
    {
        CachedFile cf=CachedFile.readFromXMLElement(element);
        String audioFileTitle=readMediaTitle(element);
        return new ImageFile(cf,audioFileTitle);
    }

    @Override
    CachedFileType getFileType()
    {
        return CachedFileType.Image;
    }

}
