package com.stevenfrew.beatprompter.midi;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.cache.FileParseError;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.cache.Tag;

import java.util.ArrayList;

public class EventOffset {
    public enum OffsetType{Milliseconds,Beats}
    public Tag mSourceTag;
    public int mAmount;
    public OffsetType mOffsetType;

    public EventOffset(String str, Tag tag, ArrayList<FileParseError> errors)
    {
        mSourceTag=tag;
        if(str==null)
            return;
        str=str.trim();
        if(str.isEmpty())
            return;
        try
        {
            mAmount=Integer.parseInt(str);
            mOffsetType=OffsetType.Milliseconds;
        }
        catch(Exception e)
        {
            // Might be in the beat format
            int diff=0;
            boolean bErrorAdded=false;
            for(int f=0;f<str.length();++f) {
                char c=str.charAt(f);
                if (c == '<')
                    --diff;
                else if (c == '>')
                    ++diff;
                else if(!bErrorAdded){
                    bErrorAdded=true;
                    errors.add(new FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.non_beat_characters_in_midi_offset)));
                }
            }
            mAmount=diff;
            mOffsetType=OffsetType.Beats;
        }
        if(Math.abs(mAmount)>16 && mOffsetType==OffsetType.Beats)
        {
            mAmount=(Math.abs(mAmount)/mAmount)*16;
            errors.add(new FileParseError(tag, BeatPrompterApplication.getResourceString(R.string.max_midi_offset_exceeded)));
        }
        else if(Math.abs(mAmount)>10000 && mOffsetType==OffsetType.Milliseconds)
        {
            mAmount=(Math.abs(mAmount)/mAmount)*10000;
            errors.add(new FileParseError(tag,BeatPrompterApplication.getResourceString(R.string.max_midi_offset_exceeded)));
        }
    }
}
