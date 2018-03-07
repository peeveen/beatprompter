package com.stevenfrew.beatprompter;

class MIDIProgramChangeMessage extends MIDIOutgoingMessage {

    private static byte PROGRAM_CHANGE_MESSAGE_BYTE=(byte)0xc0;

    MIDIProgramChangeMessage(int value,int channel)
    {
        super(mergeMessageByteWithChannel(PROGRAM_CHANGE_MESSAGE_BYTE,(byte)channel),(byte)value);
    }
}
