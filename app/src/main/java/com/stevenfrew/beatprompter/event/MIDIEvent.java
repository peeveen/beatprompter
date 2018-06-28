package com.stevenfrew.beatprompter.event;

import com.stevenfrew.beatprompter.midi.EventOffset;
import com.stevenfrew.beatprompter.midi.OutgoingMessage;

import java.util.List;

public class MIDIEvent extends BaseEvent {
    public List<OutgoingMessage> mMessages;
    public EventOffset mOffset;

    public MIDIEvent(long time,List<OutgoingMessage> messages)
    {
        super(time);
        mMessages=messages;
    }

    public MIDIEvent(long time, List<OutgoingMessage> messages, EventOffset offset)
    {
        this(time,messages);
        mOffset=offset;
    }
}
