package com.stevenfrew.beatprompter.comm.midi

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.util.getUsbDeviceMidiInterface

object UsbMidiController {
	fun initialize(
		context: Context,
		senderTask: SenderTask,
		receiverTasks: ReceiverTasks
	) {
		@SuppressLint("UnspecifiedRegisterReceiverFlag")
		if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)) {
			val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
			val intent = Intent(ACTION_USB_PERMISSION)
			intent.setPackage(context.packageName)
			val permissionIntent = PendingIntent.getBroadcast(
				context,
				0,
				intent,
				PendingIntent.FLAG_MUTABLE
			)

			val filter = IntentFilter().apply {
				addAction(ACTION_USB_PERMISSION)
				addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
				addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
			}

			val receiver = UsbBroadcastReceiver(senderTask, receiverTasks, manager, permissionIntent)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
				context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
			else
				context.registerReceiver(receiver, filter)

			attemptUsbMidiConnection(manager, permissionIntent)
		}
	}

	internal fun attemptUsbMidiConnection(manager: UsbManager, permissionIntent: PendingIntent) {
		if (BeatPrompter.preferences.midiConnectionTypes.contains(ConnectionType.USBOnTheGo)) {
			val list = manager.deviceList
			if (list != null && list.size > 0) {
				val devObjects = list.values
				for (devObj in devObjects) {
					val dev = devObj as UsbDevice
					if (dev.getUsbDeviceMidiInterface() != null) {
						manager.requestPermission(dev, permissionIntent)
						break
					}
				}
			}
		}
	}

	internal const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
}

