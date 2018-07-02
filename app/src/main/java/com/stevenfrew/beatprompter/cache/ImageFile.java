package com.stevenfrew.beatprompter.cache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ImageFile extends CachedCloudFile
{
    public final static String IMAGEFILE_ELEMENT_TAG_NAME="imagefile";

    ImageFile(CloudDownloadResult result) throws InvalidBeatPrompterFileException
    {
        super(result);
        verifyImageFile();
    }

    ImageFile(Element element)
    {
        super(element);
    }

    public void writeToXML(Document doc, Element parent)
    {
        Element imageFileElement = doc.createElement(IMAGEFILE_ELEMENT_TAG_NAME);
        super.writeToXML(imageFileElement);
        parent.appendChild(imageFileElement);
    }

    private void verifyImageFile() throws InvalidBeatPrompterFileException
    {
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            Bitmap bitmap=BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
            if(bitmap==null)
                throw new InvalidBeatPrompterFileException(SongList.mSongListInstance.getString(R.string.could_not_read_image_file)+": "+mName);
        }
        catch(Exception e)
        {
            throw new InvalidBeatPrompterFileException(SongList.mSongListInstance.getString(R.string.could_not_read_image_file)+": "+mName);
        }
    }
}
