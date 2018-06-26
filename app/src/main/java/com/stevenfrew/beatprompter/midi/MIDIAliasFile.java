package com.stevenfrew.beatprompter.midi;

import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.cache.CachedCloudFile;
import com.stevenfrew.beatprompter.cache.FileParseError;
import com.stevenfrew.beatprompter.cache.InvalidBeatPrompterFileException;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MIDIAliasFile
{
    public String mAliasSetName=null;
    public ArrayList<MIDIAlias> mAliases=new ArrayList<>();
    public ArrayList<FileParseError> mErrors=new ArrayList<>();

    private MIDIAliasFile(String aliasSetName, ArrayList<MIDIAlias> aliases, ArrayList<FileParseError> errors) {
        mAliasSetName=aliasSetName;
        mAliases=aliases;
        mErrors=errors;
    }

    private MIDIAliasFile(MIDIAliasFile maf)
    {
        mAliases=maf.mAliases;
        mErrors=maf.mErrors;
        mAliasSetName=maf.mAliasSetName;
    }

    public MIDIAliasFile(File file,String storageID) throws InvalidBeatPrompterFileException {
        this(readAliasFile(file,storageID));
    }

    private static String getMidiAliasDefinitionFromLine(String line,int lineNumber)
    {
        return CachedCloudFile.getTokenValue(line, lineNumber, "midi_alias");
    }

    private static MIDIAliasFile readAliasFile(File file, String storageName) throws InvalidBeatPrompterFileException {
        try {
            return readAliasFile(new BufferedReader(new InputStreamReader(new FileInputStream(file))),storageName);
        }
        catch(IOException ioe)
        {
            throw new InvalidBeatPrompterFileException(String.format(SongList.mSongListInstance.getString(R.string.not_a_valid_midi_alias_file), storageName));
        }
    }

    private static MIDIAliasFile readAliasFile(BufferedReader br,String filename) throws InvalidBeatPrompterFileException
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
                    aliasFilename=CachedCloudFile.getTokenValue(line,lineNumber,"midi_aliases");
                    if((aliasFilename==null)||(aliasFilename.trim().length()==0)) {
                        throw new InvalidBeatPrompterFileException(String.format(SongList.mSongListInstance.getString(R.string.not_a_valid_midi_alias_file), aliasFilename));
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
                                errors.add(new FileParseError(lineNumber, SongList.mSongListInstance.getString(R.string.midi_alias_name_contains_more_than_two_parts)));
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
                                    errors.add(new FileParseError(lineNumber, SongList.mSongListInstance.getString(R.string.midi_alias_without_a_name)));
                            }
                        } else {
                            ArrayList<MIDIAliasMessage> aliasMessages = parseAliasMessage(line,lineNumber,aliases,errors);
                            if (aliasMessages != null)
                                currentMessages.addAll(aliasMessages);
                            else
                                errors.add(new FileParseError(lineNumber,SongList.mSongListInstance.getString(R.string.midi_message_not_understood)));
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
            throw new InvalidBeatPrompterFileException(String.format(SongList.mSongListInstance.getString(R.string.not_a_valid_midi_alias_file), filename));
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

    private static ArrayList<MIDIAliasMessage> parseAliasMessage(String line,int lineNumber,ArrayList<MIDIAlias> currentAliases,ArrayList<FileParseError> errors)
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
                                maps.add(new MIDIAliasParameter(paramBit.trim()));
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
                                        errors.add(new FileParseError(lineNumber, SongList.mSongListInstance.getString(R.string.channel_must_be_last_parameter)));
                                    else
                                        --numberOfParametersSupplied;
                                }
                            }
                        }
                    }
                    if(bits.length>2)
                        errors.add(new FileParseError(lineNumber,SongList.mSongListInstance.getString(R.string.midi_alias_message_contains_more_than_two_parts)));
                    else {
                        if (tagName.equalsIgnoreCase("midi_send"))
                        {
                            ArrayList<MIDIAliasMessage> simpleList=new ArrayList<>();
                            simpleList.add(new MIDIAliasMessage(maps));
                            return simpleList;
                        } else {
                            // Does it refer to an existing alias?
                            MIDIAlias matchedAlias = null;
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
                                        fixedMessages.add(message.resolveMIDIAliasMessage(maps));
                                    return fixedMessages;
                                }
                                catch(Exception e)
                                {
                                    errors.add(new FileParseError(lineNumber,e.getMessage()));
                                }
                            }
                            else
                                errors.add(new FileParseError(lineNumber,SongList.mSongListInstance.getString(R.string.unknown_midi_directive)));
                        }
                    }
                }
                else
                    errors.add(new FileParseError(lineNumber,SongList.mSongListInstance.getString(R.string.empty_tag)));
            }
            else
                errors.add(new FileParseError(lineNumber,SongList.mSongListInstance.getString(R.string.badly_formed_tag)));
        }
        else
            errors.add(new FileParseError(lineNumber,SongList.mSongListInstance.getString(R.string.badly_formed_tag)));
        return null;
    }

}
