package com.stevenfrew.beatprompter;

import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

class MIDIAliasCachedFile extends CachedFile
{
    final static String MIDIALIASFILE_ELEMENT_TAG_NAME="midialiases";

    private MIDIAliasFile mAliasFile;

    MIDIAliasCachedFile(Context context, DownloadedFile downloadedFile,ArrayList<MIDIAlias> defaultAliases) throws InvalidBeatPrompterFileException
    {
        super(downloadedFile);
        mAliasFile=new MIDIAliasFile(context,mFile,mStorageName,defaultAliases);
    }

    private MIDIAliasCachedFile(Context context, CachedFile cachedFile,ArrayList<MIDIAlias> defaultAliases) throws IOException {
        super(cachedFile);
        mAliasFile=new MIDIAliasFile(context,mFile,mStorageName,defaultAliases);
    }

    void writeToXML(Document doc, Element parent)
    {
        Element aliasFileElement = doc.createElement(MIDIALIASFILE_ELEMENT_TAG_NAME);
        super.writeToXML(aliasFileElement);
        parent.appendChild(aliasFileElement);
    }

    static MIDIAliasCachedFile readFromXMLElement(Context context,Element element,ArrayList<MIDIAlias> defaultAliases) throws IOException
    {
        CachedFile cf=CachedFile.readFromXMLElement(element);
        return new MIDIAliasCachedFile(context,cf,defaultAliases);
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
    CachedFileType getFileType()
    {
        return CachedFileType.MIDIAliases;
    }
}
