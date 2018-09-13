package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import java.io.IOException

/**
 * This class is a thread that runs when the app is in BlueToothServer mode. It listens for connections from
 * paired clients, and creates an output socket from each connection. Any events broadcast from this instance
 * of the app will be sent to all output sockets.
 */
class ServerThread internal constructor(private val mBluetoothAdapter: BluetoothAdapter) : Thread() {
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
                            Log.d(BeatPrompterApplication.TAG, "Created the Bluetooth server socket.")
                        } catch (e: IOException) {
                            Log.e(BeatPrompterApplication.TAG, "Error creating Bluetooth server socket.", e)
                        }
                    }
                    serverSocket = mmServerSocket
                }

                // If a connection was accepted
                // Do work to manage the connection (in a separate thread)
                Log.d(BeatPrompterApplication.TAG, "Looking for a client connection.")
                serverSocket?.accept(5000)?.also{
                    Log.d(BeatPrompterApplication.TAG, "Found a client connection.")
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
                Log.d(BeatPrompterApplication.TAG, "Closing Bluetooth server socket.")
                mmServerSocket?.close()
                Log.d(BeatPrompterApplication.TAG, "Closed Bluetooth server socket.")
            } catch (e: IOException) {
                Log.e(BeatPrompterApplication.TAG, "Failed to close Bluetooth listener socket.", e)
            } finally {
                mmServerSocket = null
            }
        }
    }
}
