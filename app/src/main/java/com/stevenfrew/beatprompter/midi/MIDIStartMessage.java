package com.stevenfrew.beatprompter.midi;

public class MIDIStartMessage extends MIDIOutgoingMessage
{
    private final static byte[] MIDI_START_SIGNAL_BYTES=new byte[]{0x0f,MIDIMessage.MIDI_START_BYTE,0,0};
    public MIDIStartMessage()
    {
        super(MIDI_START_SIGNAL_BYTES,true);
    }
}
