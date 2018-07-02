package com.stevenfrew.beatprompter.midi;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;

public class USBInTask extends USBTask
{
    private byte[] mBuffer;
    private int mBufferSize;
    private int mIncomingChannels;
    private final Object mIncomingChannelsLock=new Object();

    public USBInTask(UsbDeviceConnection connection, UsbEndpoint endpoint, int incomingChannels)
    {
        super(connection,endpoint,true);
        mIncomingChannels=incomingChannels;
        mBuffer = new byte[mBufferSize = endpoint.getMaxPacketSize()];
        // Read all incoming data until there is none left. Basically, clear anything that
        // has been buffered before we accepted the connection.
        // On the offchance that there is a neverending barrage of data coming in at a ridiculous
        // speed, let's say 1K of data, max, to prevent lockup.
        int bufferClear=0;
        int dataRead;
        do
        {
            dataRead=connection.bulkTransfer(endpoint,mBuffer,mBufferSize,250);
            if(dataRead>0)
                bufferClear+=dataRead;
        }
        while((dataRead>0)&&(dataRead==mBufferSize)&&(!getShouldStop())&&(bufferClear<1024));
    }

    public void doWork()
    {
        boolean inSysEx=false;
        int dataRead;
        byte[] preBuffer=null;
        UsbEndpoint endpoint=getEndpoint();
        UsbDeviceConnection connection=getConnection();

        while(((dataRead=connection.bulkTransfer(endpoint,mBuffer,mBufferSize,1000))>0)&&(!getShouldStop()))
        {
            byte[] workBuffer;
            if(preBuffer==null)
                workBuffer=mBuffer;
            else
            {
                workBuffer=new byte[preBuffer.length+dataRead];
                System.arraycopy(preBuffer,0,workBuffer,0,preBuffer.length);
                System.arraycopy(mBuffer,0,workBuffer,preBuffer.length,dataRead);
                dataRead+=preBuffer.length;
                preBuffer=null;
            }
            for(int f=0;f<dataRead;++f)
            {
                byte messageByte=workBuffer[f];
                // All interesting MIDI signals have the top bit set.
                if((messageByte&0x80)!=0) {
                    if (inSysEx)
                    {
                        if (messageByte == Message.MIDI_SYSEX_END_BYTE) {
                            Log.d(MIDIController.MIDI_TAG, "Received MIDI SysEx end message.");
                            inSysEx = false;
                        }
                    }
                    else {
                        if ((messageByte == Message.MIDI_START_BYTE) || (messageByte == Message.MIDI_CONTINUE_BYTE) || (messageByte == Message.MIDI_STOP_BYTE)) {
                            // These are single byte messages.
                            try {
                                MIDIController.mMIDISongDisplayInQueue.put(new IncomingMessage(new byte[]{messageByte}));
                                if(messageByte== Message.MIDI_START_BYTE)
                                    Log.d(MIDIController.MIDI_TAG, "Received MIDI Start message.");
                                else if(messageByte== Message.MIDI_CONTINUE_BYTE)
                                    Log.d(MIDIController.MIDI_TAG, "Received MIDI Continue message.");
                                if(messageByte== Message.MIDI_STOP_BYTE)
                                    Log.d(MIDIController.MIDI_TAG, "Received MIDI Stop message.");
                            } catch (InterruptedException ie) {
                                Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to write new MIDI message to queue.", ie);
                            }
                        } else if(messageByte== Message.MIDI_SONG_POSITION_POINTER_BYTE)
                        {
                            // This message requires two additional bytes.
                            if (f < dataRead - 2)
                                try {
                                    IncomingMessage msg = new IncomingSongPositionPointerMessage(new byte[]{messageByte, workBuffer[++f], workBuffer[++f]});
                                    Log.d(MIDIController.MIDI_TAG, "Received MIDI Song Position Pointer message: " + msg.toString());
                                    MIDIController.mMIDISongDisplayInQueue.put(msg);
                                } catch (InterruptedException ie) {
                                    Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to write new MIDI message to queue.", ie);
                                }
                            else if (f < dataRead - 1)
                                preBuffer = new byte[]{messageByte, workBuffer[++f]};
                            else
                                preBuffer = new byte[]{messageByte};
                        }
                        else if (messageByte == Message.MIDI_SONG_SELECT_BYTE) {
                            if (f < dataRead - 1)
                                try {
                                    IncomingMessage msg = new IncomingMessage(new byte[]{messageByte, workBuffer[++f]});
                                    Log.d(MIDIController.MIDI_TAG, "Received MIDI SongSelect message: " + msg.toString());
                                    MIDIController.mMIDISongListInQueue.put(msg);
                                } catch (InterruptedException ie) {
                                    Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to write new MIDI message to queue.", ie);
                                }
                            else
                                preBuffer = new byte[]{workBuffer[f]};
                        } else if (messageByte == Message.MIDI_SYSEX_START_BYTE) {
                            Log.d(MIDIController.MIDI_TAG, "Received MIDI SysEx start message.");
                            inSysEx = true;
                        } else {
                            int channelsToListenTo=getIncomingChannels();
                            byte messageByteWithoutChannel = (byte) (messageByte & 0xF0);
                            // System messages start with 0xF0, we're past caring about those.
                            if(messageByteWithoutChannel!=(byte)0xF0) {
                                byte channel = (byte) (messageByte & 0x0F);
                                if ((channelsToListenTo & (1 << channel)) != 0) {
                                    if (messageByteWithoutChannel == Message.MIDI_PROGRAM_CHANGE_BYTE) {
                                        if (f < dataRead - 1)
                                            try {
                                                IncomingMessage msg = new IncomingMessage(new byte[]{messageByte, workBuffer[++f]});
                                                Log.d(MIDIController.MIDI_TAG, "Received MIDI ProgramChange message: " + msg.toString());
                                                MIDIController.mMIDISongListInQueue.put(msg);
                                            } catch (InterruptedException ie) {
                                                Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to write new MIDI message to queue.", ie);
                                           }
                                        else
                                            preBuffer = new byte[]{messageByte};
                                    } else if (messageByteWithoutChannel == Message.MIDI_CONTROL_CHANGE_BYTE) {
                                        // This message requires two additional bytes.
                                        if (f < dataRead - 2)
                                            try {
                                                IncomingMessage msg = new IncomingMessage(new byte[]{messageByte, workBuffer[++f], workBuffer[++f]});
                                                if ((msg.isLSBBankSelect()) || (msg.isMSBBankSelect())) {
                                                    Log.d(MIDIController.MIDI_TAG, "Received MIDI Control Change message: " + msg.toString());
                                                    MIDIController.mMIDISongListInQueue.put(msg);
                                                }
                                            } catch (InterruptedException ie) {
                                                Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to write new MIDI message to queue.", ie);
                                            }
                                        else if (f < dataRead - 1)
                                            preBuffer = new byte[]{messageByte, workBuffer[++f]};
                                        else
                                            preBuffer = new byte[]{messageByte};
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private int getIncomingChannels()
    {
        synchronized(mIncomingChannelsLock)
        {
            return mIncomingChannels;
        }
    }

    public void setIncomingChannels(int channels)
    {
        synchronized(mIncomingChannelsLock)
        {
            mIncomingChannels=channels;
        }
    }
}

