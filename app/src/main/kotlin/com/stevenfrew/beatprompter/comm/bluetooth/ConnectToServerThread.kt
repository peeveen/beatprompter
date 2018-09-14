package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.util.Utils
import java.io.IOException

/**
 * A thread that continuously attempts to connect to a band leader.
 */
internal class ConnectToServerThread(private val mDevice: BluetoothDevice) : Thread() {
    private var mmSocket: BluetoothSocket? = null
    private var mStop = false

    override fun run() {
        while (!mStop) {
            if (!BluetoothManager.isConnectedToServer)
                try {
                    // Connect the device through the socket. This will block
                    // until it succeeds or throws an exception, which can happen
                    // if it doesn't find anything to connect to within about 4 seconds.
                    val socket=mDevice.createRfcommSocketToServiceRecord(BluetoothManager.BLUETOOTH_UUID)
                    Log.d(BeatPrompterApplication.TAG, "Attempting to connect to a Bluetooth server on '${mDevice.name}'.")
                    socket?.connect().also{
                        // If the previous line didn't throw an IOException, then it connected OK.
                        // Do work to manage the connection (in a separate thread)
                        Log.d(BeatPrompterApplication.TAG, "Connected to a Bluetooth server on '${mDevice.name}'.")
                        mmSocket=socket
                        BluetoothManager.setServerConnection(socket!!)
                    }
                } catch (connectException: Exception) {
                    // There probably isn't a server to connect to. Wait a bit and try again.
                    Log.d(BeatPrompterApplication.TAG, "Failed to connect to a server on '${mDevice.name}'.")
                    Utils.safeThreadWait(1000)
                }
            else {
                // Already connected. Wait a bit and try/check again.
                Utils.safeThreadWait(2000)
            }
        }
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
        try {
            Log.d(BeatPrompterApplication.TAG, "Closing the server searching socket.")
            mmSocket?.close()
            Log.d(BeatPrompterApplication.TAG, "Closed the server searching socket.")
        } catch (e: IOException) {
            Log.e(BeatPrompterApplication.TAG, "Error closing Bluetooth socket.", e)
        }
    }
}