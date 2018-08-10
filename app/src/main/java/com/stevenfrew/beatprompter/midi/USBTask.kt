package com.stevenfrew.beatprompter.midi

import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import com.stevenfrew.beatprompter.Task

abstract class USBTask(private var mUsbDeviceConnection: UsbDeviceConnection?, private var mUsbEndpoint: UsbEndpoint?, initialRunningState: Boolean) : Task(initialRunningState) {
    private val connectionSync = Any()

    val connection: UsbDeviceConnection?
        get() = synchronized(connectionSync) {
            return mUsbDeviceConnection
        }

    val endpoint: UsbEndpoint?
        get() = synchronized(connectionSync) {
            return mUsbEndpoint
        }

    fun setConnection(connection: UsbDeviceConnection?, endpoint: UsbEndpoint?) {
        synchronized(connectionSync) {
            mUsbDeviceConnection = connection
            mUsbEndpoint = endpoint
        }
    }
}
