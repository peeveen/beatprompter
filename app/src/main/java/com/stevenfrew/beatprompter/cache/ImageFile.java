package com.stevenfrew.beatprompter.cache;

import android.graphics.BitmapFactory;

import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;
import com.stevenfrew.beatprompter.cloud.CloudFileType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

import java.util.Date;

public class ImageFile extends CachedCloudFile
{
    public final static String IMAGEFILE_ELEMENT_TAG_NAME="imagefile";

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

    public ImageFile(Element element)
    {
        super(element);
    }

    public void writeToXML(Document doc, Element parent)
    {
        Element imageFileElement = doc.createElement(IMAGEFILE_ELEMENT_TAG_NAME);
        super.writeToXML(imageFileElement);
        parent.appendChild(imageFileElement);
    }

    @Override
    public CloudFileType getFileType()
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
