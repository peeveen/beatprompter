package com.stevenfrew.beatprompter;

class MIDIStopMessage extends MIDIOutgoingMessage
{
    private final static byte[] MIDI_STOP_SIGNAL_BYTES=new byte[]{0x0f,MIDIMessage.MIDI_STOP_BYTE,0,0};
    MIDIStopMessage()
    {
        super(MIDI_STOP_SIGNAL_BYTES,true);
    }
}
