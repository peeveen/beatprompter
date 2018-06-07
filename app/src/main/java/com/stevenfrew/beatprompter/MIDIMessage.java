package com.stevenfrew.beatprompter;

class MIDIMessage
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
    protected MIDIMessage(MIDIValue[] bytes)
    {
        mMessageBytes = new byte[bytes.length];
        for (int f = 0; f < bytes.length; ++f)
            mMessageBytes[f] = bytes[f].mValue;
    }
    protected MIDIMessage(byte[] bytes)
    {
        mMessageBytes=bytes;
    }
    public String toString()
    {
        String strFormat="";
        for(int f=0;f<mMessageBytes.length;++f)
            strFormat+="%02X ";
        return String.format(strFormat.trim(),getMessageBytesAsObjectArray());
    }
    private Object[] getMessageBytesAsObjectArray()
    {
        Object[] byteObjects=new Object[mMessageBytes.length];
        for(int f=0;f<byteObjects.length;++f)
            byteObjects[f]=mMessageBytes[f];
        return byteObjects;
    }
    static byte mergeMessageByteWithChannel(byte message,byte channel)
    {
        return (byte)((message & 0xf0) | (channel & 0x0f));
    }
    static byte getChannelFromBitmask(int bitmask)
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
    static MIDIValue parseValue(String str)
    {
        return parseValue(str,false);
    }
    static MIDIValue parseChannelValue(String str,boolean fromTrigger)
    {
        return parseValue(str,fromTrigger,true);
    }
    static MIDIValue parseValue(String str, boolean fromTrigger)
    {
        return parseValue(str,fromTrigger,false);
    }
    private static MIDIValue parseValue(String str, boolean fromTrigger,boolean asChannel)
    {
        boolean isChannel=false;
        str=str.trim();
        if(str.startsWith("#")) {
            isChannel = true;
            str = str.substring(1);
        }
        if(fromTrigger)
            if(str.equals(MIDISongTrigger.WILDCARD_STRING))
                return new MIDIValue(MIDISongTrigger.WILDCARD_VALUE,asChannel);
        int returnVal;
        if(looksLikeHex(str))
            returnVal=parseHexInt(str);
        else
            returnVal=Integer.parseInt(str);
        if((isChannel)&&((returnVal < 1) || (returnVal > 16)))
            throw new IllegalArgumentException(SongList.getContext().getString(R.string.invalid_channel_value));
/*        else if(returnVal<0||returnVal>127)
            throw new IllegalArgumentException(context.getString(R.string.value_must_be_zero_to_onehundredtwentyseven));*/
        if(isChannel)
            --returnVal; // channels are zero-based internally.
        return new MIDIValue((byte)returnVal,isChannel);
    }
    private static int parseHexInt(String str)
    {
        str = str.toLowerCase();
        if (str.startsWith("0x"))
            str = str.substring(2);
        else if (str.endsWith("h"))
            str = str.substring(0, str.length() - 1);
        return Integer.parseInt(str,16);
    }
    static boolean looksLikeHex(String str) {
        if (str == null)
            return false;
        str = str.toLowerCase();
        boolean signifierFound = false;
        if (str.startsWith("0x")) {
            signifierFound = true;
            str = str.substring(2);
        } else if (str.endsWith("h"))
        {
            signifierFound = true;
            str = str.substring(0, str.length() - 1);
        }
        try
        {
            Integer.parseInt(str);
            // non-hex integer
            return signifierFound;
        }
        catch(Exception e)
        {
        }
        for(int f=0;f<str.length();++f) {
            char c=str.charAt(f);
            if ((!Character.isDigit(c))&&(c != 'a')&&(c != 'b')&&(c != 'c')&&(c != 'd')&&(c != 'e')&&(c != 'f'))
                return false;
        }
        return true;
    }
    private boolean isSystemCommonMessage(byte message)
    {
        return mMessageBytes.length>0 && mMessageBytes[0] == message;
    }
    private boolean isChannelVoiceMessage(byte message)
    {
        return mMessageBytes.length>0 && ((mMessageBytes[0] & (byte)0xF0)==message);
    }
    boolean isStart()
    {
        return isSystemCommonMessage(MIDI_START_BYTE);
    }
    boolean isStop()
    {
        return isSystemCommonMessage(MIDI_STOP_BYTE);
    }
    boolean isContinue()
    {
        return isSystemCommonMessage(MIDI_CONTINUE_BYTE);
    }
    boolean isSongPositionPointer()
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
