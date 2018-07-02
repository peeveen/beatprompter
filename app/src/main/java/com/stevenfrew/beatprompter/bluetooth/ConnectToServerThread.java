package com.stevenfrew.beatprompter.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

public class ConnectToServerThread extends Thread
{
    private BluetoothSocket mmSocket;
    private BluetoothDevice mDevice;
    private boolean mStop=false;
    private final Object mSocketNullLock=new Object();

    ConnectToServerThread(BluetoothDevice device) {
        mDevice=device;
    }

    public boolean isForDevice(BluetoothDevice device)
    {
        return mDevice.getAddress().equals(device.getAddress());
    }

    public void run() {
        // Cancel discovery because it will slow down the connection
        //            mBluetoothAdapter.cancelDiscovery();
        while (!mStop) {
            boolean alreadyConnected=BluetoothManager.isConnectedToServer();
            if (!alreadyConnected)
                try {
                    // Connect the device through the socket. This will block
                    // until it succeeds or throws an exception, which can happen
                    // if it doesn't find anything to connect to within about 4 seconds.
                    BluetoothSocket socket;
                    synchronized (mSocketNullLock) {
                        if (mmSocket == null) {
                            try {
                                // MY_UUID is the app's UUID string, also used by the server code
                                mmSocket = mDevice.createRfcommSocketToServiceRecord(BluetoothConstants.APP_BLUETOOTH_UUID);
                            } catch (IOException e) {
                                Log.e(BluetoothManager.BLUETOOTH_TAG, "Error creating Bluetooth socket.", e);
                            }
                        }
                        socket = mmSocket;
                    }
                    if (socket != null) {
                        socket.connect();

                        // Do work to manage the connection (in a separate thread)
                        BluetoothManager.setServerConnection(mmSocket);
                        ConnectedClientThread connectedClientThread = new ConnectedClientThread(mmSocket);
                        connectedClientThread.start();
                    }
                } catch (IOException connectException) {
                    Log.e(BluetoothManager.BLUETOOTH_TAG, "Failed to connect to a BeatPrompter band leader (there probably isn't one!)", connectException);
                }
                //                if(!mStop)
            else
                // Already connected. Wait a bit and try/check again.
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Log.w(BluetoothManager.BLUETOOTH_TAG, "Thread that maintains connection to the server was interrupted while waiting.");
                }
        }
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void stopTrying()
    {
        mStop=true;
        closeSocket();
    }

    public void closeSocket()
    {
        try
        {
            mmSocket.close();
        }
        catch (IOException e) {
            Log.e(BluetoothManager.BLUETOOTH_TAG, "Error closing Bluetooth socket.",e);
        }
        finally
        {
            synchronized (mSocketNullLock) {
                mmSocket = null;
            }
        }
    }
}


