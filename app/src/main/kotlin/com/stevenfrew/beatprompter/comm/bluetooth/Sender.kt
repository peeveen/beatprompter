package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothSocket
import com.stevenfrew.beatprompter.BeatPrompterLogger
import com.stevenfrew.beatprompter.comm.SenderBase

class Sender(private val mClientSocket: BluetoothSocket) : SenderBase(mClientSocket.remoteDevice.name) {
    override fun sendMessageData(bytes: ByteArray, length: Int) {
        BeatPrompterLogger.logComms("Sending Bluetooth messages.")
        mClientSocket.outputStream.write(if (bytes.size == length) bytes else bytes.copyOfRange(0, length))
    }

    override fun close() {
        mClientSocket.close()
    }
}