package com.stevenfrew.beatprompter;

class MIDIOutgoingMessage extends MIDIMessage {
    protected MIDIOutgoingMessage(byte byte1,byte byte2)
    {
        this(new byte[]{getCodeIndex(byte1),byte1,byte2,(byte)0});
    }
    protected MIDIOutgoingMessage(byte byte1,byte byte2,byte byte3)
    {
        this(new byte[]{getCodeIndex(byte1),byte1,byte2,byte3});
    }
    protected MIDIOutgoingMessage(byte[] bytes,boolean codeIndexPresent)
    {
        super(codeIndexPresent?bytes:appendCodeIndex(bytes));
    }
    MIDIOutgoingMessage(byte[] bytes)
    {
        super(appendCodeIndex(bytes));
    }
    MIDIOutgoingMessage(MIDIValue[] bytes)
    {
        super(appendCodeIndex(bytes));
    }
    private static byte[] appendCodeIndex(byte[] bytes)
    {
        byte[] newBytes=new byte[bytes.length+1];
        newBytes[0]=getCodeIndex(bytes[0]);
        System.arraycopy(bytes,0,newBytes,1,bytes.length);
        return newBytes;
    }
    private static byte[] appendCodeIndex(MIDIValue[] vals)
    {
        byte[] newBytes=new byte[vals.length+1];
        newBytes[0]=getCodeIndex(vals[0].mValue);
        for(int f=0;f<vals.length;++f)
            newBytes[f+1]=vals[f].mValue;
        return newBytes;
    }
    private static byte getCodeIndex(byte messageType)
    {
        // TODO: support more messages.
        return (byte)((messageType>>4) & (byte)0x0F);
    }
}
