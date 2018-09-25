package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.comm.SenderBase

class Sender(private val mClientSocket: BluetoothSocket): SenderBase(mClientSocket.remoteDevice.name) {
    override fun sendMessageData(bytes: ByteArray, length: Int) {
        Log.d(BeatPrompterApplication.TAG_COMMS, "Sending Bluetooth messages.")
        mClientSocket.outputStream.write(if(bytes.size==length) bytes else bytes.copyOfRange(0,length))
    }

    override fun close() {
        mClientSocket.close()
    }
}