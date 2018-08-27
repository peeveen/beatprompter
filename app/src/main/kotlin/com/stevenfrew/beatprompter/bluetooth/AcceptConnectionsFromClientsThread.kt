package com.stevenfrew.beatprompter.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import java.io.IOException

class AcceptConnectionsFromClientsThread internal constructor(private val mBluetoothAdapter: BluetoothAdapter) : Thread() {
    private var mmServerSocket: BluetoothServerSocket? = null
    private var mStop = false
    private val mSocketNullLock = Any()

    override fun run() {
        // Keep listening until exception occurs or a socket is returned
        while (!mStop) {
            try {
                var serverSocket: BluetoothServerSocket?=null
                synchronized(mSocketNullLock) {
                    if (mmServerSocket == null) {
                        try {
                            // MY_UUID is the app's UUID string, also used by the server code
                            mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(BeatPrompterApplication.APP_NAME, BluetoothManager.BLUETOOTH_UUID)
                        } catch (e: IOException) {
                            Log.e(BluetoothManager.BLUETOOTH_TAG, "Error creating Bluetooth socket.", e)
                        }
                    }
                    serverSocket = mmServerSocket
                }

                // If a connection was accepted
                // Do work to manage the connection (in a separate thread)
                serverSocket?.accept(2000)?.also{
                    BluetoothManager.handleConnectionFromClient(it)
                }
            } catch (e: IOException) {
                //Log.e(BLUETOOTH_TAG, "Failed to accept new Bluetooth connection.",e);
            }

        }
    }

    /** Will cancel the listening socket, and cause the thread to finish  */
    fun stopListening() {
        mStop = true
        synchronized(mSocketNullLock) {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(BluetoothManager.BLUETOOTH_TAG, "Failed to close Bluetooth listener socket.", e)
            } finally {
                mmServerSocket = null
            }
        }
    }
}
