package com.stevenfrew.beatprompter;

import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

class MIDIAliasCachedCloudFile extends CachedCloudFile
{
    final static String MIDIALIASFILE_ELEMENT_TAG_NAME="midialiases";

    private MIDIAliasFile mAliasFile;

    MIDIAliasCachedCloudFile(Context context, DownloadedFile downloadedFile,ArrayList<MIDIAlias> defaultAliases) throws InvalidBeatPrompterFileException
    {
        super(downloadedFile);
        mAliasFile=new MIDIAliasFile(context,mFile,mStorageID,defaultAliases);
    }

    private MIDIAliasCachedCloudFile(Context context, CachedCloudFile cachedFile,ArrayList<MIDIAlias> defaultAliases) throws IOException {
        super(cachedFile);
        mAliasFile=new MIDIAliasFile(context,mFile,mStorageID,defaultAliases);
    }

    void writeToXML(Document doc, Element parent)
    {
        Element aliasFileElement = doc.createElement(MIDIALIASFILE_ELEMENT_TAG_NAME);
        super.writeToXML(aliasFileElement);
        parent.appendChild(aliasFileElement);
    }

    static MIDIAliasCachedCloudFile readFromXMLElement(Context context,Element element,ArrayList<MIDIAlias> defaultAliases) throws IOException
    {
        CachedCloudFile cf=CachedCloudFile.readFromXMLElement(element);
        return new MIDIAliasCachedCloudFile(context,cf,defaultAliases);
    }

    ArrayList<FileParseError> getErrors()
    {
        return mAliasFile.mErrors;
    }

    ArrayList<MIDIAlias> getAliases()
    {
        return mAliasFile.mAliases;
    }

    String getAliasSetName()
    {
        return mAliasFile.mAliasSetName;
    }

    @Override
    CloudFileType getFileType()
    {
        return CloudFileType.MIDIAliases;
    }
}
