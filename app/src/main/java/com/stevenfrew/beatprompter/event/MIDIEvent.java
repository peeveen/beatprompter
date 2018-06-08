package com.stevenfrew.beatprompter.event;

import com.stevenfrew.beatprompter.midi.MIDIEventOffset;
import com.stevenfrew.beatprompter.midi.MIDIOutgoingMessage;

import java.util.ArrayList;

public class MIDIEvent extends BaseEvent {
    public ArrayList<MIDIOutgoingMessage> mMessages;
    public MIDIEventOffset mOffset;

    public MIDIEvent(long time,ArrayList<MIDIOutgoingMessage> messages)
    {
        super(time);
        mMessages=messages;
    }

    public MIDIEvent(long time,ArrayList<MIDIOutgoingMessage> messages,MIDIEventOffset offset)
    {
        this(time,messages);
        mOffset=offset;
    }
}
