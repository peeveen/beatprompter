package com.stevenfrew.beatprompter.midi;

import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.Utils;
import com.stevenfrew.beatprompter.cache.FileParseError;

import java.util.ArrayList;
import java.util.List;

/**
 * A value in a MIDI component definition.
 * It can be a simple byte value, (CommandValue)
 * or a partial byte value with channel specifier, (ChannelCommandValue)
 * or a reference to an argument (ArgumentValue)
 */
public abstract class Value {
    abstract byte resolve(byte[] arguments, byte channel) throws ResolutionException;
    abstract boolean matches(Value otherValue);

    public byte resolve() throws ResolutionException
    {
        return resolve(new byte[0],(byte)0);
    }

    public static Value parseValue(String strVal)
    {
        return Value.parseValue(strVal,0,0,1,new ArrayList<>());
    }

    public static Value parseChannelValue(String strVal)
    {
        if(!strVal.isEmpty())
            if(!strVal.startsWith("*"))
                if(!strVal.startsWith("#"))
                    strVal="#"+strVal;
        return parseValue(strVal);
    }

    public static Value parseValue(String strVal, int lineNumber, int argIndex,int argCount,List<FileParseError> errors)
    {
        strVal = strVal.trim();
        if(strVal.isEmpty())
            return new NoValue();
        if(strVal.equals("*"))
            return new WildcardValue();
        else if (strVal.startsWith("?")) {
            strVal = strVal.substring(1);
            try {
                int referencedArgIndex = Integer.parseInt(strVal);
                if(referencedArgIndex<0)
                    errors.add(new FileParseError(lineNumber, SongList.mSongListInstance.getString(R.string.not_a_valid_argument_index)));
                else
                    return new ArgumentValue(referencedArgIndex);
            } catch (NumberFormatException nfe) {
                errors.add(new FileParseError(lineNumber, SongList.mSongListInstance.getString(R.string.not_a_valid_argument_index)));
            }
        } else if (strVal.startsWith("#")) {
            if (argIndex < argCount - 1)
                errors.add(new FileParseError(lineNumber, SongList.mSongListInstance.getString(R.string.channel_must_be_last_parameter)));
            try {
                byte channel=Utils.parseByte(strVal.substring(1));
                if((channel<1)||(channel>16))
                    errors.add(new FileParseError(lineNumber,SongList.mSongListInstance.getString(R.string.invalid_channel_value)));
                return new ChannelValue(channel);
            } catch (NumberFormatException nfe) {
                errors.add(new FileParseError(lineNumber, SongList.mSongListInstance.getString(R.string.not_a_valid_byte_value)));
            }
        } else if (strVal.contains("_")) {
            if(strVal.indexOf("_")!=strVal.lastIndexOf("_"))
                errors.add(new FileParseError(lineNumber,SongList.mSongListInstance.getString(R.string.multiple_underscores_in_midi_value)));
            strVal = strVal.replace('_', '0');
            try
            {
                if(Utils.looksLikeHex(strVal)) {
                    byte channelValue = Utils.parseHexByte(strVal);
                    if((channelValue&0x0F)!=0)
                        errors.add(new FileParseError(lineNumber, SongList.mSongListInstance.getString(R.string.merge_with_channel_non_zero_lower_nibble)));
                    else
                        return new ChanneledCommandValue(channelValue);
                }
                else
                    errors.add(new FileParseError(lineNumber, SongList.mSongListInstance.getString(R.string.underscore_in_decimal_value)));
            }
            catch(NumberFormatException nfe)
            {
                errors.add(new FileParseError(lineNumber, SongList.mSongListInstance.getString(R.string.not_a_valid_byte_value)));
            }
        } else if (Utils.looksLikeHex(strVal)) {
            try {
                return new CommandValue(Utils.parseHexByte(strVal));
            } catch (NumberFormatException nfe) {
                errors.add(new FileParseError(lineNumber, SongList.mSongListInstance.getString(R.string.not_a_valid_byte_value)));
            }
        } else {
            try {
                return new CommandValue(Utils.parseByte(strVal));
            } catch (NumberFormatException nfe) {
                errors.add(new FileParseError(lineNumber, SongList.mSongListInstance.getString(R.string.not_a_valid_byte_value)));
            }
        }
        return null;
    }
}
