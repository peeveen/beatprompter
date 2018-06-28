package com.stevenfrew.beatprompter.midi;

class SongSelectMessage extends OutgoingMessage {

    private static byte SONG_SELECT_MESSAGE_BYTE=(byte)0xf3;

    SongSelectMessage(int song)
    {
        super(SONG_SELECT_MESSAGE_BYTE,(byte)(song&0x7F));
    }
}
