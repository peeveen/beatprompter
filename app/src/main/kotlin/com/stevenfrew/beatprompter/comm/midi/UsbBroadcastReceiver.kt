package com.stevenfrew.beatprompter.comm.midi

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.stevenfrew.beatprompter.comm.CommunicationType
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.comm.midi.UsbMidiController.attemptUsbMidiConnection
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.util.getUsbDeviceMidiInterface

internal class UsbBroadcastReceiver(
	private val senderTask: SenderTask,
	private val receiverTasks: ReceiverTasks,
	private val manager: UsbManager,
	private val permissionIntent: PendingIntent
) : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		val action = intent.action
		when (action) {
			UsbManager.ACTION_USB_DEVICE_ATTACHED -> attemptUsbMidiConnection(
				manager,
				permissionIntent
			)

			UsbManager.ACTION_USB_DEVICE_DETACHED -> getDeviceFromIntent(intent)?.apply {
				senderTask.removeSender(deviceName)
				receiverTasks.stopAndRemoveReceiver(deviceName)
			}

			UsbMidiController.ACTION_USB_PERMISSION -> {
				synchronized(this) {
					getDeviceFromIntent(intent)?.apply {
						if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
							val midiInterface = getUsbDeviceMidiInterface()
							if (midiInterface != null) {
								val conn = manager.openDevice(this)
								if (conn != null) {
									if (conn.claimInterface(midiInterface, true)) {
										val endpointCount = midiInterface.endpointCount
										repeat(endpointCount) {
											val endPoint = midiInterface.getEndpoint(it)
											if (endPoint.direction == UsbConstants.USB_DIR_OUT)
												senderTask.addSender(
													deviceName,
													UsbSender(conn, endPoint, deviceName, CommunicationType.UsbMidi)
												)
											else if (endPoint.direction == UsbConstants.USB_DIR_IN)
												receiverTasks.addReceiver(
													deviceName,
													deviceName,
													UsbReceiver(conn, endPoint, deviceName, CommunicationType.UsbMidi)
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

	companion object {
		private fun getDeviceFromIntent(intent: Intent): UsbDevice? =
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
				intent.getParcelableExtra(
					UsbManager.EXTRA_DEVICE
				)
			else
				intent.getParcelableExtra(
					UsbManager.EXTRA_DEVICE,
					UsbDevice::class.java
				)
	}
}

