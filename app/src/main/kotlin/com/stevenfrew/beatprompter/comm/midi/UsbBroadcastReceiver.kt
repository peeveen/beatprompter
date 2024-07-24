package com.stevenfrew.beatprompter.comm.midi

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.comm.midi.UsbMidiController.attemptUsbMidiConnection
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.util.getUsbDeviceMidiInterface

internal class UsbBroadcastReceiver(
	private val mSenderTask: SenderTask,
	private val mReceiverTasks: ReceiverTasks,
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

			UsbMidiController.ACTION_USB_PERMISSION -> {
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
													UsbSender(conn, endPoint, deviceName, USB_MIDI_COMM_TYPE)
												)
											else if (endPoint.direction == UsbConstants.USB_DIR_IN)
												mReceiverTasks.addReceiver(
													deviceName,
													deviceName,
													UsbReceiver(conn, endPoint, deviceName, USB_MIDI_COMM_TYPE)
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
		private const val USB_MIDI_COMM_TYPE = "UsbMidi"

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

