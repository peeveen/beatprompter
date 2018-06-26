package com.stevenfrew.beatprompter.midi;

import java.util.ArrayList;
import java.util.List;

class MIDIAliasMessage
{
    ArrayList<MIDIAliasParameter> mParameters;
    MIDIAliasMessage(ArrayList<MIDIAliasParameter> parameters)
    {
        mParameters=parameters;
    }
    MIDIOutgoingMessage resolveMIDIMessage(MIDIValue[] parameters,byte channel)
    {
        int paramCount=mParameters.size();
        byte[] bytes=new byte[Math.max(paramCount,3)];
        for(int f=0;f<paramCount;++f)
        {
            MIDIAliasParameter map=mParameters.get(f);
            bytes[f]=map.getValue(parameters,channel);
        }
        return new MIDIOutgoingMessage(bytes);
    }
    MIDIAliasMessage resolveMIDIAliasMessage(ArrayList<MIDIAliasParameter> parameters)
    {
        ArrayList<MIDIAliasParameter> newParams=new ArrayList<>();
        for(int f=0;f<mParameters.size();++f)
        {
            MIDIAliasParameter newAliasParam=mParameters.get(f).substitute(parameters);
            newParams.add(newAliasParam);
        }
        return new MIDIAliasMessage(newParams);
    }
}
