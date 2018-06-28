package com.stevenfrew.beatprompter.midi;

public class OutgoingMessage extends Message {
    protected OutgoingMessage(byte byte1, byte byte2)
    {
        this(new byte[]{getCodeIndex(byte1),byte1,byte2,(byte)0});
    }
    protected OutgoingMessage(byte byte1, byte byte2, byte byte3)
    {
        this(new byte[]{getCodeIndex(byte1),byte1,byte2,byte3});
    }
    protected OutgoingMessage(byte[] bytes, boolean codeIndexPresent)
    {
        super(codeIndexPresent?bytes:appendCodeIndex(bytes));
    }
    public OutgoingMessage(byte[] bytes)
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
    private static byte getCodeIndex(byte messageType)
    {
        // TODO: support more messages.
        return (byte)((messageType>>4) & (byte)0x0F);
    }
}
