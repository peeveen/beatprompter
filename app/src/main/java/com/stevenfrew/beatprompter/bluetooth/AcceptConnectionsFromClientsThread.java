package com.stevenfrew.beatprompter.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

public class AcceptConnectionsFromClientsThread extends Thread
{
    private BluetoothServerSocket mmServerSocket=null;
    private boolean mStop=false;
    private final Object mSocketNullLock=new Object();
    private BluetoothAdapter mBluetoothAdapter;

    AcceptConnectionsFromClientsThread(BluetoothAdapter bluetoothAdapter)
    {
        mBluetoothAdapter=bluetoothAdapter;
    }

    public void run() {
        // Keep listening until exception occurs or a socket is returned
        while (!mStop)
        {
            try {
                BluetoothServerSocket serverSocket;
                BluetoothSocket acceptedSocket;
                synchronized(mSocketNullLock)
                {
                    if(mmServerSocket==null)
                    {
                        try {
                            // MY_UUID is the app's UUID string, also used by the server code
                            mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(BluetoothConstants.APP_BLUETOOTH_NAME, BluetoothConstants.APP_BLUETOOTH_UUID);
                        } catch (IOException e) {
                            Log.e(BluetoothManager.BLUETOOTH_TAG, "Error creating Bluetooth socket.",e);
                        }
                    }
                    serverSocket=mmServerSocket;
                }

                if(serverSocket!=null) {
                    acceptedSocket = serverSocket.accept(2000);
                    // If a connection was accepted
                    if (acceptedSocket != null) {
                        // Do work to manage the connection (in a separate thread)
                        BluetoothManager.handleConnectionFromClient(acceptedSocket);
                    }
                }
            } catch (IOException e) {
                //Log.e(BLUETOOTH_TAG, "Failed to accept new Bluetooth connection.",e);
            }
        }
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void stopListening()
    {
        mStop=true;
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            Log.e(BluetoothManager.BLUETOOTH_TAG, "Failed to close Bluetooth listener socket.",e);
        }
        finally
        {
            synchronized (mSocketNullLock)
            {
                mmServerSocket=null;
            }
        }
    }
}

