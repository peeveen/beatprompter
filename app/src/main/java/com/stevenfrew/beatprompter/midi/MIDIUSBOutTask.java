package com.stevenfrew.beatprompter.midi;

import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Log;

import com.stevenfrew.beatprompter.BeatPrompterApplication;

public class MIDIUSBOutTask extends MIDIUSBTask
{
    public MIDIUSBOutTask()
    {
        super(null,null,true);
    }
    public void doWork()
    {
        MIDIMessage message;
        try {
            while (((message = BeatPrompterApplication.mMIDIOutQueue.take()) != null) && (!getShouldStop()))
            {
                UsbDeviceConnection connection=getConnection();
                UsbEndpoint endpoint=getEndpoint();
                if((connection!=null)&&(endpoint!=null))
                    connection.bulkTransfer(endpoint, message.mMessageBytes, message.mMessageBytes.length, 60000);
            }
        } catch (InterruptedException ie) {
            Log.d(BeatPrompterApplication.TAG, "Interrupted while attempting to retrieve MIDI out message.", ie);
        }
    }
    void cleanup()
    {
        BeatPrompterApplication.mMIDIOutQueue.clear();
    }
}

