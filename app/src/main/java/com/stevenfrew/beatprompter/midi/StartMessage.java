package com.stevenfrew.beatprompter.midi;

class StartMessage extends OutgoingMessage
{
    private final static byte[] MIDI_START_SIGNAL_BYTES=new byte[]{0x0f, Message.MIDI_START_BYTE,0,0};
    StartMessage()
    {
        super(MIDI_START_SIGNAL_BYTES,true);
    }
}
