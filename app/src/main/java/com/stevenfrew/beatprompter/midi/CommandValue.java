package com.stevenfrew.beatprompter.midi;

/**
 * A value from a MIDI component definition.
 * This class represents a simple hardcoded byte value.
 */
public class CommandValue extends Value {
    byte mValue;
    CommandValue(byte value)
    {
        mValue=value;
    }

    @Override
    byte resolve(byte[] arguments, byte channel) {
        return mValue;
    }

    @Override
    boolean matches(Value otherValue) {
        if(otherValue instanceof CommandValue)
            return ((CommandValue) otherValue).mValue==mValue;
        return otherValue instanceof WildcardValue;
    }

    @Override
    public String toString()
    {
        return ""+mValue;
    }
}
