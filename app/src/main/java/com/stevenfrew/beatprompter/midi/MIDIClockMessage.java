package com.stevenfrew.beatprompter.midi;

public class MIDIClockMessage extends MIDIOutgoingMessage
{
    private final static byte[] MIDI_CLOCK_SIGNAL_BYTES=new byte[]{0x0f,(byte)0xf8,0,0};
    public MIDIClockMessage()
    {
        super(MIDI_CLOCK_SIGNAL_BYTES,true);
    }
}