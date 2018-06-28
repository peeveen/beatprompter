package com.stevenfrew.beatprompter.midi;

class ProgramChangeMessage extends OutgoingMessage {

    private static byte PROGRAM_CHANGE_MESSAGE_BYTE=(byte)0xc0;

    ProgramChangeMessage(int value, int channel)
    {
        super(mergeMessageByteWithChannel(PROGRAM_CHANGE_MESSAGE_BYTE,(byte)channel),(byte)value);
    }
}
