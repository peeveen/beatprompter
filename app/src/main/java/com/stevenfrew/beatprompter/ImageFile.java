package com.stevenfrew.beatprompter;

import android.graphics.BitmapFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import java.util.Date;

class ImageFile extends CachedCloudFile
{
    final static String IMAGEFILE_ELEMENT_TAG_NAME="imagefile";

    ImageFile(CloudDownloadResult result) throws InvalidBeatPrompterFileException
    {
        super(result);
        verifyImageFile();
    }

    ImageFile(File file,String storageID,String name,Date lastModified,String subfolder) throws InvalidBeatPrompterFileException
    {
        super(file,storageID,name,lastModified,subfolder);
        verifyImageFile();
    }

    ImageFile(Element element)
    {
        super(element);
    }

    void writeToXML(Document doc, Element parent)
    {
        Element imageFileElement = doc.createElement(IMAGEFILE_ELEMENT_TAG_NAME);
        super.writeToXML(imageFileElement);
        parent.appendChild(imageFileElement);
    }

    @Override
    CloudFileType getFileType()
    {
        return CloudFileType.Image;
    }

    private void verifyImageFile() throws InvalidBeatPrompterFileException
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
        }
        catch(Exception e)
        {
            throw new InvalidBeatPrompterFileException(SongList.getContext().getString(R.string.could_not_read_image_file)+": "+mName);
        }
    }
}
