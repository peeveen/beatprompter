package com.stevenfrew.beatprompter.cache;

import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.cloud.CloudDownloadResult;
import com.stevenfrew.beatprompter.midi.Alias;
import com.stevenfrew.beatprompter.midi.AliasComponent;
import com.stevenfrew.beatprompter.midi.AliasSet;
import com.stevenfrew.beatprompter.midi.RecursiveAliasComponent;
import com.stevenfrew.beatprompter.midi.SimpleAliasComponent;
import com.stevenfrew.beatprompter.midi.Value;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MIDIAliasFile extends CachedCloudFile
{
    public final static String MIDIALIASFILE_ELEMENT_TAG_NAME="midialiases";

    public AliasSet mAliasSet;
    // The errors that were found in the file.
    public ArrayList<FileParseError> mErrors=new ArrayList<>();

    MIDIAliasFile(CloudDownloadResult result) throws InvalidBeatPrompterFileException
    {
        super(result.mDownloadedFile,result.mCloudFileInfo);
        mAliasSet=readAliasFile(mFile,mID,mErrors);
    }

    MIDIAliasFile(Element element) throws InvalidBeatPrompterFileException
    {
        super(element);
        mAliasSet=readAliasFile(mFile,mID,mErrors);
    }

    @Override
    public void writeToXML(Document doc, Element parent)
    {
        Element aliasFileElement = doc.createElement(MIDIALIASFILE_ELEMENT_TAG_NAME);
        super.writeToXML(aliasFileElement);
        parent.appendChild(aliasFileElement);
    }

    private static AliasSet readAliasFile(File file, String storageName, List<FileParseError> midiParsingErrors) throws InvalidBeatPrompterFileException {
        try {
            return readAliasFile(new BufferedReader(new InputStreamReader(new FileInputStream(file))),storageName,midiParsingErrors);
        }
        catch(IOException ioe)
        {
            throw new InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, storageName));
        }
    }

    private static AliasSet readAliasFile(BufferedReader br, String filename, List<FileParseError> midiParsingErrors) throws InvalidBeatPrompterFileException
    {
        try
        {
            String line;
            int lineNumber=0;
            String currentAliasName=null;
            List<AliasComponent> currentAliasComponents=new ArrayList<>();
            String aliasFilename=null;
            boolean isMidiAliasFile=false;
            ArrayList<Alias> aliases=new ArrayList<>();
            while((line=br.readLine())!=null)
            {
                ++lineNumber;
                line=line.trim();
                if(line.startsWith("#"))
                    continue;
                if(line.isEmpty())
                {
                    if(currentAliasName!=null) {
                        aliases.add(new Alias(currentAliasName, currentAliasComponents));
                        currentAliasName = null;
                        currentAliasComponents = new ArrayList<>();
                    }
                }
                if(!isMidiAliasFile) {
                    aliasFilename=CachedCloudFile.getTokenValue(line,lineNumber,"midi_aliases");
                    if((aliasFilename==null)||(aliasFilename.trim().length()==0)) {
                        throw new InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, aliasFilename));
                    }
                    isMidiAliasFile=true;
                }
                else
                {
                    // OK, we have a line of content, and we're definitely in a MIDI alias file.
                    if(currentAliasName!=null) {
                        AliasComponent component=parseAliasComponent(line,lineNumber,midiParsingErrors);
                        if(component!=null)
                            currentAliasComponents.add(component);
                    }
                    else {
                        currentAliasName = CachedCloudFile.getTokenValue(line, lineNumber, "midi_alias");
                        if (currentAliasName != null) {
                            if (currentAliasName.contains(":"))
                                midiParsingErrors.add(new FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.midi_alias_name_contains_more_than_two_parts)));
                            if (currentAliasName.isEmpty()) {
                                midiParsingErrors.add(new FileParseError(lineNumber, BeatPrompterApplication.getResourceString(R.string.midi_alias_without_a_name)));
                                currentAliasName=null;
                            }
                        }
                    }
                }
            }
            if (currentAliasName != null)
                aliases.add(new Alias(currentAliasName, currentAliasComponents));
            if(aliasFilename!=null)
                return new AliasSet(aliasFilename,aliases);
            else
                throw new InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, filename));
        }
        catch(IOException ioe)
        {
            throw new InvalidBeatPrompterFileException(BeatPrompterApplication.getResourceString(R.string.not_a_valid_midi_alias_file, filename));
        }
        finally
        {
            try
            {
                if(br!=null)
                    br.close();
            }
            catch(IOException ioe)
            {
                Log.e(BeatPrompterApplication.TAG,"Failed to close set list file",ioe);
            }
        }
    }

    private static AliasComponent parseAliasComponent(String line,int lineNumber,List<FileParseError> errors)
    {
        int bracketStart=line.indexOf("{");
        if(bracketStart!=-1)
        {
            int bracketEnd=line.indexOf("}",bracketStart);
            if(bracketEnd!=-1)
            {
                String contents=line.substring(bracketStart+1,bracketEnd-bracketStart).trim().toLowerCase();
                if(contents.length()>0) {
                    String bits[] = contents.split(":");
                    List<Value> componentArgs=new ArrayList<>();
                    String tagName = bits[0].trim();
                    if(bits.length>2)
                        errors.add(new FileParseError(lineNumber,BeatPrompterApplication.getResourceString(R.string.midi_alias_message_contains_more_than_two_parts)));
                    else if (bits.length > 1) {
                        String params = bits[1].trim();
                        String[] paramBits=params.split(",");
                        int paramCounter=0;
                        for(String paramBit:paramBits) {
                            Value aliasValue = Value.Companion.parseValue(paramBit,lineNumber,paramCounter++,paramBits.length,errors);
                            if(aliasValue!=null)
                                componentArgs.add(aliasValue);
                        }
                    }

                    if (tagName.equalsIgnoreCase("midi_send"))
                        return new SimpleAliasComponent(componentArgs);
                    else
                        return new RecursiveAliasComponent(tagName,componentArgs);
                }
                else
                    errors.add(new FileParseError(lineNumber,BeatPrompterApplication.getResourceString(R.string.empty_tag)));
            }
            else
                errors.add(new FileParseError(lineNumber,BeatPrompterApplication.getResourceString(R.string.badly_formed_tag)));
        }
        else
            errors.add(new FileParseError(lineNumber,BeatPrompterApplication.getResourceString(R.string.badly_formed_tag)));
        return null;
    }
}
