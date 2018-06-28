package com.stevenfrew.beatprompter.midi;

public class ChannelValue extends CommandValue {
    ChannelValue(byte channel)
    {
        super(channel);
    }

    @Override
    byte resolve(byte[] arguments, byte channel) {
        return mValue;
    }

    @Override
    public String toString()
    {
        return "#"+mValue;
    }

}
