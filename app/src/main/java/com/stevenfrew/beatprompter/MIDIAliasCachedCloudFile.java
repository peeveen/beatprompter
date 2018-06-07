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

    public MIDIAliasCachedCloudFile(CloudDownloadResult result,ArrayList<MIDIAlias> defaultAliases) throws InvalidBeatPrompterFileException
    {
        super(result.mDownloadedFile,result.mCloudFileInfo);
        mAliasFile=new MIDIAliasFile(mFile,mStorageID,defaultAliases);
    }

    MIDIAliasCachedCloudFile(File file,String storageID,String title,Date lastModified,String subfolder,ArrayList<MIDIAlias> defaultAliases) throws InvalidBeatPrompterFileException
    {
        super(file,storageID,title,lastModified,subfolder);
        mAliasFile=new MIDIAliasFile(mFile,mStorageID,defaultAliases);
    }

    void writeToXML(Document doc, Element parent)
    {
        Element aliasFileElement = doc.createElement(MIDIALIASFILE_ELEMENT_TAG_NAME);
        super.writeToXML(aliasFileElement);
        parent.appendChild(aliasFileElement);
    }

    MIDIAliasCachedCloudFile(Element element,ArrayList<MIDIAlias> defaultAliases) throws InvalidBeatPrompterFileException
    {
        super(element);
        mAliasFile=new MIDIAliasFile(mFile,mStorageID,defaultAliases);
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
