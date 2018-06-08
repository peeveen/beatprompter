package com.stevenfrew.beatprompter.midi;

class MIDISongSelectMessage extends MIDIOutgoingMessage {

    private static byte SONG_SELECT_MESSAGE_BYTE=(byte)0xf3;

    MIDISongSelectMessage(int song)
    {
        super(SONG_SELECT_MESSAGE_BYTE,(byte)(song&0x7F));
    }
}
