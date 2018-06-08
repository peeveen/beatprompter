package com.stevenfrew.beatprompter.midi;

import java.util.ArrayList;

public class MIDIAlias
{
    public String mName;
    public int mParamCount;
    ArrayList<MIDIAliasMessage> mMessages;

    MIDIAlias(String name,ArrayList<MIDIAliasMessage> messages)
    {
        mParamCount=0;
        mName=name;
        for(MIDIAliasMessage message:messages)
            for(MIDIAliasParameter param:message.mParameters)
                mParamCount = Math.max(mParamCount, param.getParameterIndexReference());
        mMessages=messages;
    }

    public ArrayList<MIDIOutgoingMessage> resolveMessages(MIDIValue[] parameters,byte channel)
    {
        ArrayList<MIDIOutgoingMessage> outMessages=new ArrayList<>();
        for(MIDIAliasMessage mam:mMessages)
            outMessages.add(mam.resolveMIDIMessage(parameters,channel));
        return outMessages;
    }
}
