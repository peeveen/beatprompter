package com.stevenfrew.beatprompter.midi;

/**
 * A value from a MIDI component definition.
 * This class represents a simple hardcoded byte value.
 */
class CommandValue extends ByteValue {
    CommandValue(byte value)
    {
        super(value);
    }
}
