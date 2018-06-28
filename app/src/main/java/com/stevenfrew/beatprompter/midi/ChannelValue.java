package com.stevenfrew.beatprompter.midi;

public class ChannelValue extends ByteValue {
    ChannelValue(byte channel)
    {
        super(channel);
    }

    @Override
    public String toString()
    {
        return "#"+mValue;
    }

}
