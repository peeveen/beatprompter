package com.stevenfrew.beatprompter;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

class MIDIAliasFile
{
    String mAliasSetName=null;
    ArrayList<MIDIAlias> mAliases=new ArrayList<>();
    ArrayList<FileParseError> mErrors=new ArrayList<>();

    private MIDIAliasFile(String aliasSetName, ArrayList<MIDIAlias> aliases, ArrayList<FileParseError> errors) {
        mAliasSetName=aliasSetName;
        mAliases=aliases;
        mErrors=errors;
    }

    private MIDIAliasFile(MIDIAliasFile maf) throws InvalidBeatPrompterFileException
    {
        mAliases=maf.mAliases;
        mErrors=maf.mErrors;
        mAliasSetName=maf.mAliasSetName;
    }

    MIDIAliasFile(Context context,BufferedReader br,String aliasSetName) throws InvalidBeatPrompterFileException
    {
        this(readAliasFile(context,br,aliasSetName,new ArrayList<MIDIAlias>()));
    }

    MIDIAliasFile(Context context,File file,String storageName,ArrayList<MIDIAlias> defaultAliases) throws InvalidBeatPrompterFileException {
        this(readAliasFile(context,file,storageName,defaultAliases));
    }

    private static String getMidiAliasDefinitionFromLine(String line,int lineNumber)
    {
        return CachedFile.getTokenValue(line, lineNumber, "midi_alias");
    }

    private static MIDIAliasFile readAliasFile(Context context, File file, String storageName,ArrayList<MIDIAlias> defaultAliases) throws InvalidBeatPrompterFileException {
        try {
            return readAliasFile(context,new BufferedReader(new InputStreamReader(new FileInputStream(file))),storageName,defaultAliases);
        }
        catch(IOException ioe)
        {
            throw new InvalidBeatPrompterFileException(String.format(context.getString(R.string.not_a_valid_midi_alias_file), storageName));
        }
    }

    private static MIDIAliasFile readAliasFile(Context context,BufferedReader br,String filename,ArrayList<MIDIAlias> defaultAliases) throws InvalidBeatPrompterFileException
    {
        try
        {
            String line;
            int lineNumber=0;
            String currentAliasName=null;
            String aliasFilename=null;
            ArrayList<MIDIAliasMessage> currentMessages=new ArrayList<>();
            boolean isMidiAliasFile=false;
            ArrayList<MIDIAlias> aliases=new ArrayList<>();
            ArrayList<FileParseError> errors=new ArrayList<>();
            while((line=br.readLine())!=null)
            {
                ++lineNumber;
                line=line.trim();
                if(line.startsWith("#"))
                    continue;
                if(line.isEmpty())
                    continue;
                if(!isMidiAliasFile) {
                    aliasFilename=CachedFile.getTokenValue(line,lineNumber,"midi_aliases");
                    if((aliasFilename==null)||(aliasFilename.trim().length()==0)) {
                        throw new InvalidBeatPrompterFileException(String.format(context.getString(R.string.not_a_valid_midi_alias_file), aliasFilename));
                    }
                    isMidiAliasFile=true;
                }
                else
                {
                    line=line.toLowerCase();
                    if(line.length()==0) {
                        if (currentAliasName != null) {
                            aliases.add(new MIDIAlias(currentAliasName, currentMessages));
                            currentAliasName = null;
                            currentMessages=new ArrayList<>();
                        }
                    }
                    else {
                        String aliasName = getMidiAliasDefinitionFromLine(line, lineNumber);
                        if (aliasName != null) {
                            if(aliasName.contains(":"))
                                errors.add(new FileParseError(lineNumber, context.getString(R.string.midi_alias_name_contains_more_than_two_parts)));
                            else {
                                aliasName = aliasName.trim();
                                if (currentAliasName != null) {
                                    aliases.add(new MIDIAlias(currentAliasName, currentMessages));
                                    currentAliasName = null;
                                    currentMessages=new ArrayList<>();
                                }
                                if (aliasName.length() > 0)
                                    currentAliasName = aliasName;
                                else
                                    errors.add(new FileParseError(lineNumber, context.getString(R.string.midi_alias_without_a_name)));
                            }
                        } else {
                            ArrayList<MIDIAliasMessage> aliasMessages = parseAliasMessage(context,line,lineNumber,aliases,errors,defaultAliases);
                            if (aliasMessages != null)
                                currentMessages.addAll(aliasMessages);
                            else
                                errors.add(new FileParseError(lineNumber,context.getString(R.string.midi_message_not_understood)));
                        }
                    }
                }
            }
            if (currentAliasName != null)
                aliases.add(new MIDIAlias(currentAliasName, currentMessages));
            return new MIDIAliasFile(aliasFilename,aliases,errors);
        }
        catch(IOException ioe)
        {
            throw new InvalidBeatPrompterFileException(String.format(context.getString(R.string.not_a_valid_midi_alias_file), filename));
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

    private static ArrayList<MIDIAliasMessage> parseAliasMessage(Context context,String line,int lineNumber,ArrayList<MIDIAlias> currentAliases,ArrayList<FileParseError> errors,ArrayList<MIDIAlias> defaultAliases)
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
                    ArrayList<MIDIAliasParameter> maps=new ArrayList<>();
                    String tagName = bits[0].trim();
                    int numberOfParametersSupplied=0;
                    if (bits.length > 1) {
                        String params = bits[1].trim();
                        String[] paramBits=params.split(",");
                        try {
                            for(String paramBit:paramBits)
                                maps.add(new MIDIAliasParameter(context, paramBit.trim()));
                        }
                        catch(Exception e)
                        {
                            errors.add(new FileParseError(lineNumber,e.getMessage()));
                            return null;
                        }
                        numberOfParametersSupplied=maps.size();
                        if(maps.size()>0)
                        {
                            // Validation.
                            for(int f=0;f<maps.size();++f)
                            {
                                if(maps.get(f).isChannelReference()) {
                                    if (f < maps.size() - 1)
                                        errors.add(new FileParseError(lineNumber, context.getString(R.string.channel_must_be_last_parameter)));
                                    else
                                        --numberOfParametersSupplied;
                                }
                            }
                        }
                    }
                    if(bits.length>2)
                        errors.add(new FileParseError(lineNumber,context.getString(R.string.midi_alias_message_contains_more_than_two_parts)));
                    else {
                        if (tagName.equalsIgnoreCase("midi_send"))
                        {
                            ArrayList<MIDIAliasMessage> simpleList=new ArrayList<>();
                            simpleList.add(new MIDIAliasMessage(maps));
                            return simpleList;
                        } else {
                            // Does it refer to an existing alias?
                            MIDIAlias matchedAlias = null;
                            for (MIDIAlias alias : defaultAliases)
                                if (tagName.equalsIgnoreCase(alias.mName))
                                    if(alias.mParamCount==numberOfParametersSupplied)
                                    {
                                        matchedAlias = alias;
                                        break;
                                    }
                            if(matchedAlias==null)
                                for (MIDIAlias alias : currentAliases)
                                    if (tagName.equalsIgnoreCase(alias.mName))
                                        if(alias.mParamCount==numberOfParametersSupplied)
                                        {
                                            matchedAlias = alias;
                                            break;
                                        }
                            if(matchedAlias!=null)
                            {
                                ArrayList<MIDIAliasMessage> messages=matchedAlias.mMessages;
                                ArrayList<MIDIAliasMessage> fixedMessages=new ArrayList<>();
                                try {
                                    for(MIDIAliasMessage message:messages)
                                        fixedMessages.add(message.resolveMIDIAliasMessage(context,maps));
                                    return fixedMessages;
                                }
                                catch(Exception e)
                                {
                                    errors.add(new FileParseError(lineNumber,e.getMessage()));
                                }
                            }
                            else
                                errors.add(new FileParseError(lineNumber,context.getString(R.string.unknown_midi_directive)));
                        }
                    }
                }
                else
                    errors.add(new FileParseError(lineNumber,context.getString(R.string.empty_tag)));
            }
            else
                errors.add(new FileParseError(lineNumber,context.getString(R.string.badly_formed_tag)));
        }
        else
            errors.add(new FileParseError(lineNumber,context.getString(R.string.badly_formed_tag)));
        return null;
    }

}
