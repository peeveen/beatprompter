package com.stevenfrew.beatprompter.midi;

public class ChanneledCommandValue extends CommandValue {
    ChanneledCommandValue(byte value)
    {
        super(value);
    }

    @Override
    byte resolve(byte[] arguments, byte channel) {
        return (byte)((mValue&0xF0) | (channel & 0x0F));
    }

    @Override
    boolean matches(Value otherValue) {
        if(otherValue instanceof ChanneledCommandValue)
            return ((ChanneledCommandValue) otherValue).mValue==mValue;
        return otherValue instanceof WildcardValue;
    }

    @Override
    public String toString()
    {
        String strHex=Integer.toHexString(mValue);
        return "0x"+strHex.substring(0,1)+"_";
    }
}
