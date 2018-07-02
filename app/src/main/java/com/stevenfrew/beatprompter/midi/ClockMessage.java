package com.stevenfrew.beatprompter.midi;

class ClockMessage extends OutgoingMessage
{
    private final static byte[] MIDI_CLOCK_SIGNAL_BYTES=new byte[]{0x0f,(byte)0xf8,0,0};
    ClockMessage()
    {
        super(MIDI_CLOCK_SIGNAL_BYTES,true);
    }
}
