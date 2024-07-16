package com.stevenfrew.beatprompter.comm.midi

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.util.getUsbDeviceMidiInterface

class UsbMidiController(
	context: Context,
	internal val mSenderTask: SenderTask,
	internal val mReceiverTasks: ReceiverTasks
) {
	private inner class UsbReceiver(
		private val mManager: UsbManager,
		private val mPermissionIntent: PendingIntent
	) : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val action = intent.action
			when (action) {
				UsbManager.ACTION_USB_DEVICE_ATTACHED -> attemptUsbMidiConnection(
					mManager,
					mPermissionIntent
				)

				UsbManager.ACTION_USB_DEVICE_DETACHED -> getDeviceFromIntent(intent)?.apply {
					mSenderTask.removeSender(deviceName)
					mReceiverTasks.stopAndRemoveReceiver(deviceName)
				}

				ACTION_USB_PERMISSION -> {
					synchronized(this) {
						getDeviceFromIntent(intent)?.apply {
							if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
								val midiInterface = getUsbDeviceMidiInterface()
								if (midiInterface != null) {
									val conn = mManager.openDevice(this)
									if (conn != null) {
										if (conn.claimInterface(midiInterface, true)) {
											val endpointCount = midiInterface.endpointCount
											repeat(endpointCount) {
												val endPoint = midiInterface.getEndpoint(it)
												if (endPoint.direction == UsbConstants.USB_DIR_OUT)
													mSenderTask.addSender(
														deviceName,
														UsbSender(conn, endPoint, deviceName)
													)
												else if (endPoint.direction == UsbConstants.USB_DIR_IN)
													mReceiverTasks.addReceiver(
														deviceName,
														deviceName,
														UsbReceiver(conn, endPoint, deviceName)
													)
												EventRouter.sendEventToSongList(Events.CONNECTION_ADDED, deviceName)
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	init {
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

			val receiver = UsbReceiver(manager, permissionIntent)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
				context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
			else
				context.registerReceiver(receiver, filter)

			attemptUsbMidiConnection(manager, permissionIntent)
		}
	}

	private fun getDeviceFromIntent(intent: Intent): UsbDevice? {
		return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
			intent.getParcelableExtra(
				UsbManager.EXTRA_DEVICE
			)
		else
			intent.getParcelableExtra(
				UsbManager.EXTRA_DEVICE,
				UsbDevice::class.java
			)
	}

	private fun attemptUsbMidiConnection(manager: UsbManager, permissionIntent: PendingIntent) {
		if (Preferences.midiConnectionType == ConnectionType.USBOnTheGo) {
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

	companion object {
		private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
	}
}

