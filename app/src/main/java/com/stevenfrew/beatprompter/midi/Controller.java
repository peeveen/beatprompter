package com.stevenfrew.beatprompter.midi;

import java.util.concurrent.ArrayBlockingQueue;

public class Controller {
    public static final String MIDI_TAG="midi";
    private static final int MIDI_QUEUE_SIZE=1024;
    public static ArrayBlockingQueue<OutgoingMessage> mMIDIOutQueue=new ArrayBlockingQueue<>(MIDI_QUEUE_SIZE);
    public static ArrayBlockingQueue<IncomingMessage> mMIDISongDisplayInQueue=new ArrayBlockingQueue<>(MIDI_QUEUE_SIZE);
    public static ArrayBlockingQueue<IncomingMessage> mMIDISongListInQueue=new ArrayBlockingQueue<>(MIDI_QUEUE_SIZE);
    public static byte[] mMidiBankMSBs=new byte[16];
    public static byte[] mMidiBankLSBs=new byte[16];
}
