package com.stevenfrew.beatprompter.cache;

import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;
import com.stevenfrew.beatprompter.cloud.CloudFileType;
import com.stevenfrew.beatprompter.midi.MIDIAlias;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

public class MIDIAliasFile extends CachedCloudFile
{
    public final static String MIDIALIASFILE_ELEMENT_TAG_NAME="midialiases";

    private com.stevenfrew.beatprompter.midi.MIDIAliasFile mAliasFile;

    public MIDIAliasFile(CloudDownloadResult result, ArrayList<MIDIAlias> defaultAliases) throws InvalidBeatPrompterFileException
    {
        super(result.mDownloadedFile,result.mCloudFileInfo);
        mAliasFile=new com.stevenfrew.beatprompter.midi.MIDIAliasFile(mFile,mStorageID,defaultAliases);
    }

    MIDIAliasFile(File file, String storageID, String title, Date lastModified, String subfolder, ArrayList<MIDIAlias> defaultAliases) throws InvalidBeatPrompterFileException
    {
        super(file,storageID,title,lastModified,subfolder);
        mAliasFile=new com.stevenfrew.beatprompter.midi.MIDIAliasFile(mFile,mStorageID,defaultAliases);
    }

    @Override
    public void writeToXML(Document doc, Element parent)
    {
        Element aliasFileElement = doc.createElement(MIDIALIASFILE_ELEMENT_TAG_NAME);
        super.writeToXML(aliasFileElement);
        parent.appendChild(aliasFileElement);
    }

    public MIDIAliasFile(Element element, ArrayList<MIDIAlias> defaultAliases) throws InvalidBeatPrompterFileException
    {
        super(element);
        mAliasFile=new com.stevenfrew.beatprompter.midi.MIDIAliasFile(mFile,mStorageID,defaultAliases);
    }

    public ArrayList<FileParseError> getErrors()
    {
        return mAliasFile.mErrors;
    }

    public ArrayList<MIDIAlias> getAliases()
    {
        return mAliasFile.mAliases;
    }

    public String getAliasSetName()
    {
        return mAliasFile.mAliasSetName;
    }

    @Override
    public CloudFileType getFileType()
    {
        return CloudFileType.MIDIAliases;
    }
}