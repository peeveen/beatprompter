package com.stevenfrew.beatprompter.midi;

public abstract class ByteValue extends Value {
    byte mValue;
    ByteValue(byte value)
    {
        mValue=value;
    }

    @Override
    byte resolve(byte[] arguments, byte channel) {
        return mValue;
    }

    @Override
    boolean matches(Value otherValue) {
        if(otherValue instanceof ByteValue)
            return ((ByteValue) otherValue).mValue==mValue;
        return otherValue instanceof WildcardValue;
    }

    @Override
    public String toString()
    {
        return ""+mValue;
    }
}
