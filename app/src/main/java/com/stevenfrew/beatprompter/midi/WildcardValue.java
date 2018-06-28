package com.stevenfrew.beatprompter.midi;

/**
 * Represents a value that matches anything else.
 */
public class WildcardValue extends Value {

    @Override
    byte resolve(byte[] arguments, byte channel) {
        return 0;
    }

    @Override
    boolean matches(Value otherValue) {
        return true;
    }

    @Override
    public String toString()
    {
        return "*";
    }
}
