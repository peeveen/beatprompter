package com.stevenfrew.beatprompter;

import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

class CachedFile {
    File mFile;
    String mStorageName;
    String mSubfolder;
    Date mLastModified;
    String mTitle;

    private final static String CACHED_FILE_PATH_ATTRIBUTE_NAME="path";
    private final static String CACHED_FILE_STORAGE_NAME_ATTRIBUTE_NAME="storageName";
    private final static String CACHED_FILE_LAST_MODIFIED_ATTRIBUTE_NAME="lastModified";
    private final static String CACHED_FILE_SUBFOLDER_ATTRIBUTE_NAME="subfolder";

    CachedFile(File file, String storageName, Date lastModified,String subfolder)
    {
        mFile=file;
        mTitle=file.getName();
        mStorageName=storageName;
        mLastModified=lastModified;
        mSubfolder=subfolder;
    }

    CachedFile(DownloadedFile downloadedFile)
    {
        this(downloadedFile.mFile,downloadedFile.mCloudFileInfo.mStorageID,downloadedFile.mCloudFileInfo.mLastModified,downloadedFile.mCloudFileInfo.mSubfolder);
    }

    CachedFile(CachedFile cachedFile)
    {
        this(cachedFile.mFile,cachedFile.mStorageName,cachedFile.mLastModified,cachedFile.mSubfolder);
    }

    static String getTokenValue(String line,int lineNumber, String... tokens)
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

    void writeToXML(Element element)
    {
        element.setAttribute(CACHED_FILE_PATH_ATTRIBUTE_NAME,mFile.getAbsolutePath());
        element.setAttribute(CACHED_FILE_STORAGE_NAME_ATTRIBUTE_NAME,mStorageName);
        element.setAttribute(CACHED_FILE_LAST_MODIFIED_ATTRIBUTE_NAME,""+mLastModified.getTime());
        element.setAttribute(CACHED_FILE_SUBFOLDER_ATTRIBUTE_NAME,mSubfolder==null?"":mSubfolder);
    }

    static CachedFile readFromXMLElement(Element element)
    {
        String path=element.getAttribute(CACHED_FILE_PATH_ATTRIBUTE_NAME);
        String storageName=element.getAttribute(CACHED_FILE_STORAGE_NAME_ATTRIBUTE_NAME);
        String lastModifiedString=element.getAttribute(CACHED_FILE_LAST_MODIFIED_ATTRIBUTE_NAME);
        String subfolder=element.getAttribute(CACHED_FILE_SUBFOLDER_ATTRIBUTE_NAME);
        if(subfolder==null)
            subfolder="";
        File file=new File(path);
        Date lastModified=new Date(Long.parseLong(lastModifiedString));
        return new CachedFile(file,storageName,lastModified,subfolder);
    }

    CachedFileType getFileType()
    {
        return CachedFileType.None;
    }
}