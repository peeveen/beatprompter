package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.stevenfrew.beatprompter.util.Utils
import java.io.IOException

/**
 * A thread that continuously attempts to connect to a band leader.
 */
internal class ConnectToServerThread(private val mDevice: BluetoothDevice) : Thread() {
    private var mmSocket: BluetoothSocket? = null
    private var mStop = false
    private val mSocketNullLock = Any()

    override fun run() {
        while (!mStop) {
            if (!BluetoothManager.isConnectedToServer)
                try {
                    // Connect the device through the socket. This will block
                    // until it succeeds or throws an exception, which can happen
                    // if it doesn't find anything to connect to within about 4 seconds.
                    var socket: BluetoothSocket?=null
                    synchronized(mSocketNullLock) {
                        if (mmSocket == null) {
                            try {
                                // MY_UUID is the app's UUID string, also used by the server code
                                mmSocket = mDevice.createRfcommSocketToServiceRecord(BluetoothManager.BLUETOOTH_UUID)
                            } catch (e: IOException) {
                                Log.e(BluetoothManager.BLUETOOTH_TAG, "Error creating Bluetooth socket.", e)
                            }

                        }
                        socket = mmSocket
                    }
                    socket?.connect().also{
                        // If the previous line didn't throw an IOException, then it connected OK.
                        // Do work to manage the connection (in a separate thread)
                        BluetoothManager.setServerConnection(socket!!)
                    }
                } catch (connectException: IOException) {
                    // There probably isn't a server to connect to. Wait a bit and try again.
                    Utils.safeThreadWait(100)
                }
            else
                // Already connected. Wait a bit and try/check again.
                Utils.safeThreadWait(2000)
        }
    }

    /**
     * Got a connection, so add it to the pile of maintained connections.
     */
    private fun startConnectionThread(socket:BluetoothSocket)
    {
    }

    /**
     *  Will cancel an in-progress connection, and close the socket
     */
    internal fun stopTrying() {
        mStop = true
        closeSocket()
    }

    /**
     * Closes the watching socket.
     */
    private fun closeSocket() {
        synchronized(mSocketNullLock) {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(BluetoothManager.BLUETOOTH_TAG, "Error closing Bluetooth socket.", e)
            } finally {
                mmSocket = null
            }
        }
    }
}