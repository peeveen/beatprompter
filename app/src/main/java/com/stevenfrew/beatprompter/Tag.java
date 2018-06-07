package com.stevenfrew.beatprompter;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

class Tag
{
    public String mName;
    String mValue;
    int mLineNumber;
    boolean mChordTag;
    int mPosition;

    private static final String[] COLOR_TAG_NAMES=new String[]
            {
                    "backgroundcolour","bgcolour","backgroundcolor","bgcolor",
                    "pulsecolour","beatcolour","pulsecolor","beatcolor",
                    "lyriccolour","lyricscolour","lyriccolor","lyricscolor",
                    "chordcolour","chordcolor",
                    "commentcolour","commentcolor",
                    "beatcountercolour","beatcountercolor",
            };
    static final HashSet<String> COLOR_TAGS=new HashSet<>(Arrays.asList(COLOR_TAG_NAMES));
    private static final String[] ONE_SHOT_TAG_NAMES=new String[]
            {
                    "title","t","artist","a","subtitle","st",
                    "count","trackoffset","time","midi_song_select_trigger","midi_program_change_trigger"
            };
    static final HashSet<String> ONE_SHOT_TAGS=new HashSet<>(Arrays.asList(ONE_SHOT_TAG_NAMES));

    private Tag(boolean chordTag,String str,int lineNumber,int position)
    {
        mChordTag=chordTag;
        int colonIndex=str.indexOf(":");
        int spaceIndex=str.indexOf(" ");
        if(colonIndex==-1)
            colonIndex=spaceIndex;
        if(colonIndex==-1)
        {
            mName = chordTag?str:str.toLowerCase();
            mValue="";
        }
        else
        {
            mName=chordTag?str:str.substring(0,colonIndex).toLowerCase();
            mValue=str.substring(colonIndex+1);
        }
        mLineNumber=lineNumber;
        mPosition=position;
    }

    static MIDIEvent getMIDIEventFromTag(long time,Tag tag,ArrayList<MIDIAlias> aliases,byte defaultChannel,ArrayList<FileParseError> parseErrors) throws Exception
    {
        ArrayList<MIDIOutgoingMessage> outArray=null;
        String val=tag.mValue.trim();
        MIDIEventOffset eventOffset=null;
        if(val.isEmpty())
        {
            // A MIDI tag of {blah;+33} ends up with "blah;+33" as the tag name. Fix it here.
            if(tag.mName.contains(";"))
            {
                String bits[]=tag.mName.split(";");
                if(bits.length>2)
                    parseErrors.add(new FileParseError(tag, SongList.getContext().getString(R.string.multiple_semi_colons_in_midi_tag)));
                if(bits.length>1) {
                    eventOffset=new MIDIEventOffset(SongList.getContext(),bits[1].trim(),tag,parseErrors);
                    tag.mName=bits[0].trim();
                }
            }
        }
        else
        {
            String firstSplitBits[] = (val.length() == 0 ? new String[0] : val.split(";"));
            if (firstSplitBits.length > 1) {
                if (firstSplitBits.length > 2)
                    parseErrors.add(new FileParseError(tag, SongList.getContext().getString(R.string.multiple_semi_colons_in_midi_tag)));
                val = firstSplitBits[0].trim();
                eventOffset = new MIDIEventOffset(SongList.getContext(), firstSplitBits[1].trim(), tag, parseErrors);
            }
        }
        String[] bits=(val.length()==0?new String[0]:val.split(","));
        MIDIValue[] paramBytes=new MIDIValue[bits.length];
        Exception parseValueException=null;
        try {
            for (int f = 0; f < bits.length; ++f)
                paramBytes[f] = MIDIMessage.parseValue(bits[f].trim());
        }
        catch(Exception e)
        {
            parseValueException=e;
        }
        boolean lastParamIsChannel=false;
        byte channel=defaultChannel;
        for(int f=0;f<paramBytes.length;++f)
            if(paramBytes[f].mChannelSpecifier)
                if(f==paramBytes.length-1)
                    lastParamIsChannel=true;
                else
                    parseErrors.add(new FileParseError(tag,SongList.getContext().getString(R.string.channel_must_be_last_parameter)));
        if(lastParamIsChannel) {
            MIDIValue lastParam= paramBytes[paramBytes.length - 1];
            channel = lastParam.mValue;
            MIDIValue[] paramBytesWithoutChannel=new MIDIValue[paramBytes.length-1];
            System.arraycopy(paramBytes,0,paramBytesWithoutChannel,0,paramBytesWithoutChannel.length);
            paramBytes=paramBytesWithoutChannel;
        }
        for(MIDIAlias alias:aliases)
            if(alias.mName.equalsIgnoreCase(tag.mName))
                if(alias.mParamCount==paramBytes.length)
                    outArray=alias.resolveMessages(paramBytes,channel);
        if(tag.mName.equals("midi_send"))
        {
            if(paramBytes[0].isChannelledMessage())
            {
                byte b=paramBytes[0].mValue;
                b&=(byte)0xF0;
                b|=channel;
                paramBytes[0]=new MIDIValue(b);
            }
            MIDIOutgoingMessage customMidiMessage=new MIDIOutgoingMessage(paramBytes);
            outArray=new ArrayList<>();
            outArray.add(customMidiMessage);
        }
        if((outArray==null)||(outArray.isEmpty())) {
            parseErrors.add(new FileParseError(tag, SongList.getContext().getString(R.string.unknown_midi_directive)));
            return null;
        }
        if(parseValueException!=null) {
            parseErrors.add(new FileParseError(tag, parseValueException.getMessage()));
            return null;
        }
        return new MIDIEvent(time,outArray,eventOffset);
    }

    static int getIntegerValueFromTag(Tag tag,int min,int max,int defolt, ArrayList<FileParseError> parseErrors)
    {
        int val;
        try
        {
            val = Integer.parseInt(tag.mValue);
            if (val<min)
            {
                parseErrors.add(new FileParseError(tag,String.format(SongList.getContext().getString(R.string.intValueTooLow),min,val)));
                val = min;
            }
            else if(val>max)
            {
                parseErrors.add(new FileParseError(tag,String.format(SongList.getContext().getString(R.string.intValueTooHigh),max,val)));
                val = max;
            }
        }
        catch(NumberFormatException nfe)
        {
            parseErrors.add(new FileParseError(tag,String.format(SongList.getContext().getString(R.string.intValueUnreadable),tag.mValue,defolt)));
            val=defolt;
        }
        return val;
    }

    static int getDurationValueFromTag(Tag tag,int min,int max,int defolt,boolean trackLengthAllowed,ArrayList<FileParseError> parseErrors)
    {
        int val;
        try
        {
            val=Utils.parseDuration(tag.mValue,trackLengthAllowed);
            if ((val<min)&&(val!=Utils.TRACK_AUDIO_LENGTH_VALUE))
            {
                parseErrors.add(new FileParseError(tag,String.format(SongList.getContext().getString(R.string.intValueTooLow),min,val)));
                val = min;
            }
            else if(val>max)
            {
                parseErrors.add(new FileParseError(tag,String.format(SongList.getContext().getString(R.string.intValueTooHigh),max,val)));
                val = max;
            }
        }
        catch(NumberFormatException nfe)
        {
            parseErrors.add(new FileParseError(tag,String.format(SongList.getContext().getString(R.string.durationValueUnreadable),tag.mValue,defolt)));
            val=defolt;
        }
        return val;
    }

    static double getDoubleValueFromTag(Tag tag,double min,double max,double defolt, ArrayList<FileParseError> parseErrors)
    {
        double val;
        try
        {
            val = Double.parseDouble(tag.mValue);
            if (val<min)
            {
                parseErrors.add(new FileParseError(tag,String.format(SongList.getContext().getString(R.string.doubleValueTooLow),min,val)));
                val = min;
            }
            else if(val>max)
            {
                parseErrors.add(new FileParseError(tag,String.format(SongList.getContext().getString(R.string.doubleValueTooHigh),max,val)));
                val = max;
            }
        }
        catch(NumberFormatException nfe)
        {
            parseErrors.add(new FileParseError(tag,String.format(SongList.getContext().getString(R.string.doubleValueUnreadable),tag.mValue,defolt)));
            val=defolt;
        }
        return val;
    }

    static int getColourValueFromTag(Tag tag,int defolt, ArrayList<FileParseError> parseErrors)
    {
        try
        {
            return Color.parseColor(tag.mValue);
        }
        catch(IllegalArgumentException iae)
        {
            try
            {
                return Color.parseColor("#"+tag.mValue);
            }
            catch(IllegalArgumentException iae2)
            {
                String defaultString=("000000"+Integer.toHexString(defolt));
                defaultString=defaultString.substring(defaultString.length()-6);
                parseErrors.add(new FileParseError(tag,String.format(SongList.getContext().getString(R.string.colorValueUnreadable),tag.mValue,defaultString)));
            }
        }
        return defolt;
    }

    static MIDISongTrigger getSongTriggerFromTag(Tag tag,ArrayList<FileParseError> parseErrors)
    {
        try
        {
            return MIDISongTrigger.parse( tag.mValue, tag.mName.equals("midi_song_select_trigger"));
        }
        catch(Exception e)
        {
            parseErrors.add(new FileParseError(tag,e.getMessage()));
        }
        return null;
    }

    static String extractTags(String line, int lineNumber, ArrayList<Tag> tagsOut)
    {
        tagsOut.clear();
        String lineOut="";
        int directiveStart=line.indexOf("{");
        int chordStart=line.indexOf("[");
        while((directiveStart!=-1)||(chordStart!=-1))
        {
            int start;
            if(directiveStart!=-1)
                if((chordStart!=-1)&&(chordStart<directiveStart))
                    start=chordStart;
                else
                    start=directiveStart;
            else
                start=chordStart;
            String tagCloser=(start==directiveStart?"}":"]");
            int end=line.indexOf(tagCloser,start+1);
            if(end!=-1)
            {
                String contents=line.substring(start+1,end).trim();
                lineOut+=line.substring(0,start);
                line=line.substring(end+tagCloser.length());
                end=0;
                if(contents.trim().length()>0)
                    tagsOut.add(new Tag(start==chordStart,contents,lineNumber, lineOut.length()));
            }
            else
                end=start+1;
            directiveStart=line.indexOf("{",end);
            chordStart=line.indexOf("[",end);
        }
        lineOut+=line;
        return lineOut;
    }
}
