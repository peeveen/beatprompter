package com.stevenfrew.beatprompter.midi;

/**
 * Represents "no value", used for matching against nothing.
 */
public class NoValue extends Value {
    @Override
    byte resolve(byte[] arguments, byte channel) {
        return 0;
    }

    @Override
    boolean matches(Value otherValue) {
        return false;
    }

    @Override
    public String toString()
    {
        return "";
    }
}
