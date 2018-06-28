package com.stevenfrew.beatprompter.midi;

public class IncomingSongPositionPointerMessage extends IncomingMessage
{
    IncomingSongPositionPointerMessage(byte[] bytes)
    {
        super(bytes);
    }
    public int getMIDIBeat()
    {
        int firstHalf=mMessageBytes[2];
        firstHalf<<=7;
        int secondHalf=mMessageBytes[1];
        return firstHalf|secondHalf;
    }
}
