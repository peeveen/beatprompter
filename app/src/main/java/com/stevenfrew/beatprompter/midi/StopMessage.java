package com.stevenfrew.beatprompter.midi;

public class StopMessage extends OutgoingMessage
{
    private final static byte[] MIDI_STOP_SIGNAL_BYTES=new byte[]{0x0f, Message.MIDI_STOP_BYTE,0,0};
    public StopMessage()
    {
        super(MIDI_STOP_SIGNAL_BYTES,true);
    }
}
