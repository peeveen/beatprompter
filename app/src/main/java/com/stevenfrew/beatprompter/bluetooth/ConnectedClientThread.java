package com.stevenfrew.beatprompter.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

class ConnectedClientThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;

    ConnectedClientThread(BluetoothSocket socket) {
        mmSocket = socket;
        InputStream tmpIn = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            Log.e(BluetoothManager.BLUETOOTH_TAG, "Failed to open Bluetooth input stream.",e);
        }

        mmInStream = tmpIn;
    }

    public void run() {
        byte[] buffer = new byte[2048];  // buffer store for the stream
        int bytes; // bytes returned from read()
        int bufferContentsLength=0;

        // Keep listening to the InputStream until an exception occurs
        while (mmSocket.isConnected())
        {
            try
            {
                // Read from the InputStream
                bytes = mmInStream.read(buffer,bufferContentsLength,2048-bufferContentsLength);
                // Send the obtained bytes to the UI activity
                if(bytes>0) {
                    bufferContentsLength += bytes;
                    while (bufferContentsLength > 0) {
                        try {
                            BluetoothMessage btm = BluetoothMessage.fromBytes(buffer);
                            int messageLength = btm.mMessageLength;
                            bufferContentsLength -= messageLength;
                            System.arraycopy(buffer, messageLength, buffer, 0, bufferContentsLength);
                            BluetoothManager.routeBluetoothMessage(btm);
                        } catch (NotEnoughBluetoothDataException nebde) {
                            // Read again!
                            Log.d(BluetoothManager.BLUETOOTH_TAG, "Not enough data in the Bluetooth buffer to create a fully formed message, waiting for more data.");
                            break;
                        }
                    }
                }
            }
            catch (IOException e)
            {
                Log.e(BluetoothManager.BLUETOOTH_TAG, "Failed to read or route the received Bluetooth message.",e);
            }
        }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(BluetoothManager.BLUETOOTH_TAG, "Failed to close the Bluetooth input socket.",e);
        }
    }
}


