package com.stevenfrew.beatprompter.cache;

import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;
import com.stevenfrew.beatprompter.cloud.CloudFileInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

public abstract class CachedCloudFile extends CloudFileInfo {

    public File mFile;

    private final static String CACHED_FILE_PATH_ATTRIBUTE_NAME="path";

    CachedCloudFile(File file, String id, String name, Date lastModified,String subfolder)
    {
        super(id,name,lastModified,subfolder);
        mFile=file;
    }

    CachedCloudFile(File file, CloudFileInfo cloudFileInfo)
    {
        super(cloudFileInfo.mID,cloudFileInfo.mName,cloudFileInfo.mLastModified,cloudFileInfo.mSubfolder);
        mFile=file;
    }

    CachedCloudFile(CloudDownloadResult result)
    {
        this(result.mDownloadedFile,result.mCloudFileInfo);
    }

    CachedCloudFile(Element element)
    {
        super(element);
        mFile=new File(element.getAttribute(CACHED_FILE_PATH_ATTRIBUTE_NAME));
    }

    public static String getTokenValue(String line,int lineNumber, String... tokens)
    {
        ArrayList<String> values=getTokenValues(line, lineNumber, tokens);
        if(values.size()==0)
            return null;
        return values.get(values.size() - 1);
    }

    static ArrayList<String> getTokenValues(String line,int lineNumber, String... tokens)
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

    static boolean containsToken(String line,int lineNumber,String tokenToFind)
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
        super.writeToXML(element);
        element.setAttribute(CACHED_FILE_PATH_ATTRIBUTE_NAME,mFile.getAbsolutePath());
    }

    public static CachedCloudFile createCachedCloudFile(CloudDownloadResult result)
    {
        try
        {
            return new AudioFile(result);
        }
        catch(InvalidBeatPrompterFileException ioe)
        {
            try {
                return new ImageFile(result);
            }
            catch(InvalidBeatPrompterFileException ibpfe1)
            {
                try {
                    return new MIDIAliasFile(result);
                }
                catch(InvalidBeatPrompterFileException ibpfe2)
                {
                    try {
                        return new SongFile(result);
                    }
                    catch(InvalidBeatPrompterFileException ibpfe3)
                    {
                        try {
                            return new SetListFile(result);
                        }
                        catch(InvalidBeatPrompterFileException ibpfe4)
                        {
                            // Not any kind of file we're interested in?
                            // We don't want this file.
                        }
                    }
                }
            }
        }
        return null;
    }
}