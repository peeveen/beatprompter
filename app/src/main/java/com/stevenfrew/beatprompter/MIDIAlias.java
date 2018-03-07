package com.stevenfrew.beatprompter;

import java.util.ArrayList;

class MIDIAlias
{
    String mName;
    int mParamCount;
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

    ArrayList<MIDIOutgoingMessage> resolveMessages(MIDIValue[] parameters,byte channel)
    {
        ArrayList<MIDIOutgoingMessage> outMessages=new ArrayList<>();
        for(MIDIAliasMessage mam:mMessages)
            outMessages.add(mam.resolveMIDIMessage(parameters,channel));
        return outMessages;
    }
}
