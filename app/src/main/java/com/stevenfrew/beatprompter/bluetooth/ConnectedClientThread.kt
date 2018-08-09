package com.stevenfrew.beatprompter.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream

internal class ConnectedClientThread(private val mmSocket: BluetoothSocket) : Thread() {
    private val mmInStream: InputStream?

    init {
        var tmpIn: InputStream? = null

        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
            tmpIn = mmSocket.inputStream
        } catch (e: IOException) {
            Log.e(BluetoothManager.BLUETOOTH_TAG, "Failed to open Bluetooth input stream.", e)
        }

        mmInStream = tmpIn
    }

    override fun run() {
        val buffer = ByteArray(2048)  // buffer store for the stream
        var bytes: Int // bytes returned from read()
        var bufferContentsLength = 0

        // Keep listening to the InputStream until an exception occurs
        while (mmSocket.isConnected) {
            try {
                // Read from the InputStream
                bytes = mmInStream!!.read(buffer, bufferContentsLength, 2048 - bufferContentsLength)
                // Send the obtained bytes to the UI activity
                if (bytes > 0) {
                    bufferContentsLength += bytes
                    while (bufferContentsLength > 0) {
                        try {
                            val btm = BluetoothMessage.fromBytes(buffer)
                            if(btm!=null) {
                                val messageLength = btm.mMessageLength
                                bufferContentsLength -= messageLength
                                System.arraycopy(buffer, messageLength, buffer, 0, bufferContentsLength)
                                BluetoothManager.routeBluetoothMessage(btm)
                            }
                        } catch (nebde: NotEnoughBluetoothDataException) {
                            // Read again!
                            Log.d(BluetoothManager.BLUETOOTH_TAG, "Not enough data in the Bluetooth buffer to create a fully formed message, waiting for more data.")
                            break
                        }

                    }
                }
            } catch (e: IOException) {
                Log.e(BluetoothManager.BLUETOOTH_TAG, "Failed to read or route the received Bluetooth message.", e)
            }

        }
    }

    /* Call this from the main activity to shutdown the connection */
    fun cancel() {
        try {
            mmSocket.close()
        } catch (e: IOException) {
            Log.e(BluetoothManager.BLUETOOTH_TAG, "Failed to close the Bluetooth input socket.", e)
        }

    }
}