package com.stevenfrew.beatprompter;

import java.util.ArrayList;

class MIDIEvent extends BaseEvent {
    ArrayList<MIDIOutgoingMessage> mMessages;
    MIDIEventOffset mOffset;

    MIDIEvent(long time,ArrayList<MIDIOutgoingMessage> messages)
    {
        super(time);
        mMessages=messages;
    }

    MIDIEvent(long time,ArrayList<MIDIOutgoingMessage> messages,MIDIEventOffset offset)
    {
        this(time,messages);
        mOffset=offset;
    }
}
