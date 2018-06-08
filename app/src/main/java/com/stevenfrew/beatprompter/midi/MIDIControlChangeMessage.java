package com.stevenfrew.beatprompter.midi;

class MIDIControlChangeMessage extends MIDIOutgoingMessage {

    private static byte CONTROL_CHANGE_MESSAGE_BYTE=(byte)0xb0;
    static byte BANK_SELECT_MSB_BYTE=(byte)0;
    static byte BANK_SELECT_LSB_BYTE=(byte)32;

    MIDIControlChangeMessage(byte controller,MIDIValue value,byte channel)
    {
        super(mergeMessageByteWithChannel(CONTROL_CHANGE_MESSAGE_BYTE,channel),controller,value.mValue);
    }
}
