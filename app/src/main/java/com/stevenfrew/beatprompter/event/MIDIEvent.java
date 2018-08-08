package com.stevenfrew.beatprompter.event;

import com.stevenfrew.beatprompter.midi.OutgoingMessage;
import com.stevenfrew.beatprompter.midi.EventOffset;

import java.util.ArrayList;
import java.util.List;

public class MIDIEvent extends BaseEvent {
    public List<OutgoingMessage> mMessages;
    public EventOffset mOffset;

    public MIDIEvent(long time,List<OutgoingMessage> messages)
    {
        super(time);
        mMessages=messages;
    }

    private MIDIEvent(long time,OutgoingMessage message)
    {
        super(time);
        mMessages=new ArrayList<>();
        mMessages.add(message);
    }

    public MIDIEvent(long time, List<OutgoingMessage> messages, EventOffset offset)
    {
        this(time,messages);
        mOffset=offset;
    }

    public MIDIEvent(long time, OutgoingMessage message, EventOffset offset)
    {
        this(time,message);
        mOffset=offset;
    }
}
