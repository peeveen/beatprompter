package com.stevenfrew.beatprompter.midi;

class ControlChangeMessage extends OutgoingMessage {

    private static byte CONTROL_CHANGE_MESSAGE_BYTE=(byte)0xb0;
    static byte BANK_SELECT_MSB_BYTE=(byte)0;
    static byte BANK_SELECT_LSB_BYTE=(byte)32;

    ControlChangeMessage(byte controller, byte value, byte channel)
    {
        super(mergeMessageByteWithChannel(CONTROL_CHANGE_MESSAGE_BYTE,channel),controller,value);
    }
}
