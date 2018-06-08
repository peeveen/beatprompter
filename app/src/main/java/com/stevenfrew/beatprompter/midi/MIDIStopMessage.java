package com.stevenfrew.beatprompter.midi;

public class MIDIStopMessage extends MIDIOutgoingMessage
{
    private final static byte[] MIDI_STOP_SIGNAL_BYTES=new byte[]{0x0f,MIDIMessage.MIDI_STOP_BYTE,0,0};
    public MIDIStopMessage()
    {
        super(MIDI_STOP_SIGNAL_BYTES,true);
    }
}
