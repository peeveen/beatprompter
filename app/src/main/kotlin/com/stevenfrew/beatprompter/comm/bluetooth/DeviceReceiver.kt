package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask

class DeviceReceiver(
	private val senderTask: SenderTask,
	private val receiverTasks: ReceiverTasks,
) : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
			// Something has disconnected.
			(if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
				intent.getParcelableExtra(
					BluetoothDevice.EXTRA_DEVICE
				)
			else
				intent.getParcelableExtra(
					BluetoothDevice.EXTRA_DEVICE,
					BluetoothDevice::class.java
				))?.apply {
				Logger.logComms({ "A Bluetooth device with address '$address' has disconnected." })
				receiverTasks.stopAndRemoveReceiver(address)
				senderTask.removeSender(address)
			}
		}
	}
}
