package com.stevenfrew.beatprompter.midi;

public class Message
{
    final static byte MIDI_SYSEX_START_BYTE=(byte)0xf0;
    final static byte MIDI_SONG_POSITION_POINTER_BYTE=(byte)0xf2;
    final static byte MIDI_SONG_SELECT_BYTE=(byte)0xf3;
    final static byte MIDI_START_BYTE=(byte)0xfa;
    final static byte MIDI_CONTINUE_BYTE=(byte)0xfb;
    final static byte MIDI_STOP_BYTE=(byte)0xfc;
    final static byte MIDI_SYSEX_END_BYTE=(byte)0xf7;

    final static byte MIDI_CONTROL_CHANGE_BYTE=(byte)0xb0;
    final static byte MIDI_PROGRAM_CHANGE_BYTE=(byte)0xc0;

    private final static byte MIDI_MSB_BANK_SELECT_CONTROLLER=(byte)0;
    private final static byte MIDI_LSB_BANK_SELECT_CONTROLLER=(byte)32;

    byte[] mMessageBytes;
    protected Message(byte[] bytes)
    {
        mMessageBytes=bytes;
    }
    public String toString()
    {
        StringBuilder strFormat= new StringBuilder();
        for (byte mMessageByte : mMessageBytes) strFormat.append(String.format("%02X ",mMessageByte));
        return strFormat.toString();
    }
    static byte mergeMessageByteWithChannel(byte message,byte channel)
    {
        return (byte)((message & 0xf0) | (channel & 0x0f));
    }
    public static byte getChannelFromBitmask(int bitmask)
    {
        int n=1;
        byte counter=0;
        do {
            if(n==bitmask)
                return counter;
            n<<=1;
            ++counter;
        }
        while(counter<16);
        return 0;
    }
    private boolean isSystemCommonMessage(byte message)
    {
        return mMessageBytes.length>0 && mMessageBytes[0] == message;
    }
    private boolean isChannelVoiceMessage(byte message)
    {
        return mMessageBytes.length>0 && ((mMessageBytes[0] & (byte)0xF0)==message);
    }
    public boolean isStart()
    {
        return isSystemCommonMessage(MIDI_START_BYTE);
    }
    public boolean isStop()
    {
        return isSystemCommonMessage(MIDI_STOP_BYTE);
    }
    public boolean isContinue()
    {
        return isSystemCommonMessage(MIDI_CONTINUE_BYTE);
    }
    public boolean isSongPositionPointer()
    {
        return isSystemCommonMessage(MIDI_SONG_POSITION_POINTER_BYTE);
    }
    boolean isSongSelect()
    {
        return isSystemCommonMessage(MIDI_SONG_SELECT_BYTE);
    }
    boolean isMSBBankSelect()
    {
        return isControlChange() && mMessageBytes[1] == MIDI_MSB_BANK_SELECT_CONTROLLER;
    }
    boolean isLSBBankSelect()
    {
        return isControlChange() && mMessageBytes[1] == MIDI_LSB_BANK_SELECT_CONTROLLER;
    }
    boolean isProgramChange()
    {
        return isChannelVoiceMessage(MIDI_PROGRAM_CHANGE_BYTE);
    }
    private boolean isControlChange()
    {
        return isChannelVoiceMessage(MIDI_CONTROL_CHANGE_BYTE);
    }
    int getMIDIChannel()
    {
        return mMessageBytes[0]&0x0F;
    }
    int getBankSelectValue()
    {
        return mMessageBytes[2];
    }
    int getSongSelectValue()
    {
        return mMessageBytes[2];
    }
    int getProgramChangeValue()
    {
        return mMessageBytes[1];
    }
}
