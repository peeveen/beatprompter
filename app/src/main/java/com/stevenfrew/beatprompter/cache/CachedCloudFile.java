package com.stevenfrew.beatprompter.cache;

import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;
import com.stevenfrew.beatprompter.cloud.CloudFileInfo;
import com.stevenfrew.beatprompter.cloud.CloudFileType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public abstract class CachedCloudFile extends CloudFileInfo {

    public File mFile;

    private final static String CACHED_FILE_PATH_ATTRIBUTE_NAME="path";
    private final static String CACHED_FILE_NAME_ATTRIBUTE_NAME="name";
    private final static String CACHED_FILE_STORAGE_ID_ATTRIBUTE_NAME="storageID";
    private final static String CACHED_FILE_LAST_MODIFIED_ATTRIBUTE_NAME="lastModified";
    private final static String CACHED_FILE_SUBFOLDER_ATTRIBUTE_NAME="subfolder";

    public CachedCloudFile(File file, String storageID, String name, Date lastModified,String subfolder)
    {
        super(storageID,name,lastModified,subfolder);
        mFile=file;
    }

    public CachedCloudFile(File file, CloudFileInfo cloudFileInfo)
    {
        super(cloudFileInfo.mStorageID,cloudFileInfo.mName,cloudFileInfo.mLastModified,cloudFileInfo.mSubfolder);
        mFile=file;
    }

    public CachedCloudFile(CloudDownloadResult result)
    {
        this(result.mDownloadedFile,result.mCloudFileInfo);
    }

    protected CachedCloudFile(Element element)
    {
        this(
                new File(element.getAttribute(CACHED_FILE_PATH_ATTRIBUTE_NAME)),
                element.getAttribute(CACHED_FILE_STORAGE_ID_ATTRIBUTE_NAME),
                element.getAttribute(CACHED_FILE_NAME_ATTRIBUTE_NAME),
                new Date(Long.parseLong(element.getAttribute(CACHED_FILE_LAST_MODIFIED_ATTRIBUTE_NAME))),
                element.getAttribute(CACHED_FILE_SUBFOLDER_ATTRIBUTE_NAME));
    }

    public static String getTokenValue(String line,int lineNumber, String... tokens)
    {
        ArrayList<String> values=getTokenValues(line, lineNumber, tokens);
        if(values.size()==0)
            return null;
        return values.get(values.size() - 1);
    }

    public static ArrayList<String> getTokenValues(String line,int lineNumber, String... tokens)
    {
        ArrayList<Tag> tagsOut=new ArrayList<>();
        ArrayList<String> values=new ArrayList<>();
        if(!line.trim().startsWith("#")) {
            Tag.extractTags(line, lineNumber, tagsOut);
            for (Tag tag : tagsOut)
                for (String token : tokens)
                    if (tag.mName.equals(token))
                        if (tag.mValue != null)
                            values.add(tag.mValue.trim());
        }
        return values;
    }

    public static boolean containsToken(String line,int lineNumber,String tokenToFind)
    {
        ArrayList<Tag> tagsOut=new ArrayList<>();
        if(!line.trim().startsWith("#")) {
            Tag.extractTags(line, lineNumber, tagsOut);
            for (Tag tag : tagsOut)
                if (tag.mName.equals(tokenToFind))
                    return true;
        }
        return false;
    }

    public abstract void writeToXML(Document d,Element element);

    public void writeToXML(Element element)
    {
        element.setAttribute(CACHED_FILE_NAME_ATTRIBUTE_NAME,mName);
        element.setAttribute(CACHED_FILE_PATH_ATTRIBUTE_NAME,mFile.getAbsolutePath());
        element.setAttribute(CACHED_FILE_STORAGE_ID_ATTRIBUTE_NAME,mStorageID);
        element.setAttribute(CACHED_FILE_LAST_MODIFIED_ATTRIBUTE_NAME,""+mLastModified.getTime());
        element.setAttribute(CACHED_FILE_SUBFOLDER_ATTRIBUTE_NAME,mSubfolder==null?"":mSubfolder);
    }

    public static CachedCloudFile createCachedCloudFile(CloudDownloadResult result)
    {
        try
        {
            return new SongFile(result);
        }
        catch(IOException ioe)
        {
            try {
                // Not a song file. Might be a set file?
                return new SetListFile(result);
            }
            catch(InvalidBeatPrompterFileException ibpfe1)
            {
                // Not a set list file. Might be a MIDI Alias file?
                try {
                    // Not a song file. Might be a set file?
                    return new MIDIAliasFile(result, SongList.mDefaultAliases);
                }
                catch(InvalidBeatPrompterFileException ibpfe2)
                {
                    // Not a MIDI Alias file. Might be an audio file?
                    // Not a set list file. Might be a MIDI Alias file?
                    try {
                        // Not a song file. Might be a set file?
                        return new AudioFile(result);
                    }
                    catch(InvalidBeatPrompterFileException ibpfe3)
                    {
                        // Not an Audio file. Might be an image file?
                        try {
                            // Not a song file. Might be a set file?
                            return new ImageFile(result);
                        }
                        catch(InvalidBeatPrompterFileException ibpfe4)
                        {
                            // Not an Image file.
                            // We don't want this file.
                        }
                    }
                }
            }
        }
        return null;
    }

    public abstract CloudFileType getFileType();
}