package com.stevenfrew.beatprompter;

abstract class SignalOnlyBluetoothMessage extends BluetoothMessage
{
    private byte mMessageID;
    SignalOnlyBluetoothMessage(byte messageID)
    {
        mMessageID=messageID;
    }

    SignalOnlyBluetoothMessage(byte[] bytes) throws NotEnoughBluetoothDataException
    {
        if(bytes.length<1)
            throw new NotEnoughBluetoothDataException();
        mMessageLength=1;
    }

    public byte[] getBytes()
    {
        byte[] bytes=new byte[1];
        bytes[0]=mMessageID;
        return bytes;
    }
}
