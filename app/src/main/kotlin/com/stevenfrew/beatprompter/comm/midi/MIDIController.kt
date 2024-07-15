package com.stevenfrew.beatprompter.comm.midi

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbConstants.USB_ENDPOINT_XFER_BULK
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.os.Build
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.OutgoingMessage
import com.stevenfrew.beatprompter.comm.ReceiverTask
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events

object MIDIController {
	private var mMidiUsbRegistered = false
	private var mUsbManager: UsbManager? = null
	private var mInitialised = false

	private var mNativeMidiManager: MidiManager? = null

	private var mMidiNativeDeviceListener: MidiNativeDeviceListener? = null

	private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
	private var mPermissionIntent: PendingIntent? = null

	private const val MIDI_QUEUE_SIZE = 4096

	private var mMIDIOutQueue = MIDIMessageQueue(MIDI_QUEUE_SIZE)
	private val mSenderTask = SenderTask(mMIDIOutQueue)
	private val mReceiverTasks = ReceiverTasks()

	private val mSenderTaskThread = Thread(mSenderTask).also { it.priority = Thread.MAX_PRIORITY }

	private fun addNativeDevice(nativeDeviceInfo: MidiDeviceInfo) {
		if (Preferences.midiConnectionType == ConnectionType.Native)
			if (mNativeMidiManager != null)
				mNativeMidiManager!!.openDevice(nativeDeviceInfo, mMidiNativeDeviceListener, null)
	}

	private val mUsbReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val action = intent.action
			if (UsbManager.ACTION_USB_DEVICE_ATTACHED == action) {
				attemptUsbMidiConnection()
			}
			if (UsbManager.ACTION_USB_DEVICE_DETACHED == action) {
				(if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
					intent.getParcelableExtra(
						UsbManager.EXTRA_DEVICE
					)
				else
					intent.getParcelableExtra(
						UsbManager.EXTRA_DEVICE,
						UsbDevice::class.java
					))?.apply {
					mSenderTask.removeSender(deviceName)
					mReceiverTasks.stopAndRemoveReceiver(deviceName)
				}
			} else if (ACTION_USB_PERMISSION == action) {
				synchronized(this) {
					val device = (if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
						intent.getParcelableExtra(
							UsbManager.EXTRA_DEVICE
						)
					else
						intent.getParcelableExtra(
							UsbManager.EXTRA_DEVICE,
							UsbDevice::class.java
						))
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						if (device != null) {
							val midiInterface = getDeviceMidiInterface(device)
							if (midiInterface != null) {
								val conn = mUsbManager!!.openDevice(device)
								if (conn != null) {
									if (conn.claimInterface(midiInterface, true)) {
										val endpointCount = midiInterface.endpointCount
										repeat(endpointCount) {
											val endPoint = midiInterface.getEndpoint(it)
											if (endPoint.direction == UsbConstants.USB_DIR_OUT)
												mSenderTask.addSender(
													device.deviceName,
													UsbSender(conn, endPoint, device.deviceName)
												)
											else if (endPoint.direction == UsbConstants.USB_DIR_IN)
												mReceiverTasks.addReceiver(
													device.deviceName,
													device.deviceName,
													UsbReceiver(conn, endPoint, device.deviceName)
												)
											EventRouter.sendEventToSongList(Events.CONNECTION_ADDED, device.deviceName)
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

	private fun getDeviceMidiInterface(device: UsbDevice): UsbInterface? {
		val interfaceCount = device.interfaceCount
		var fallbackInterface: UsbInterface? = null
		repeat(interfaceCount) { interfaceIndex ->
			val face = device.getInterface(interfaceIndex)
			val mainClass = face.interfaceClass
			val subclass = face.interfaceSubclass
			// Oh you f***in beauty, we've got a perfect compliant MIDI interface!
			if (mainClass == 1 && subclass == 3)
				return face
			else if (mainClass == 255 && fallbackInterface == null) {
				// Basically, go with this if:
				// It has all endpoints of type "bulk transfer"
				// and
				// The endpoints have a max packet size that is a multiplier of 4.
				val endPointCount = face.endpointCount
				var allEndpointsCheckout = true
				repeat(endPointCount) {
					val ep = face.getEndpoint(it)
					val maxPacket = ep.maxPacketSize
					val type = ep.type
					allEndpointsCheckout =
						allEndpointsCheckout and (type == USB_ENDPOINT_XFER_BULK && (maxPacket and 3) == 0)
				}
				if (allEndpointsCheckout)
					fallbackInterface = face
			}
			// Aw bollocks, we've got some vendor-specific pish.
			// Still worth trying.
		}
		return fallbackInterface
	}

	private fun attemptUsbMidiConnection() {
		if (Preferences.midiConnectionType == ConnectionType.USBOnTheGo) {
			val list = mUsbManager?.deviceList
			if (list != null && list.size > 0) {
				val devObjects = list.values
				for (devObj in devObjects) {
					val dev = devObj as UsbDevice
					if (getDeviceMidiInterface(dev) != null) {
						mUsbManager!!.requestPermission(dev, mPermissionIntent)
						break
					}
				}
			}
		}
	}

	fun initialise(application: BeatPrompter) {
		mSenderTaskThread.start()
		Task.resumeTask(mSenderTask)

		mMidiNativeDeviceListener = MidiNativeDeviceListener()
		mNativeMidiManager = application.getSystemService(Context.MIDI_SERVICE) as? MidiManager
		mNativeMidiManager?.apply {
			registerDeviceCallback(mMidiNativeDeviceListener, null)
			devices?.forEach {
				addNativeDevice(it)
			}
		}

		mUsbManager = application.getSystemService(Context.USB_SERVICE) as? UsbManager
		val intent = Intent(ACTION_USB_PERMISSION)
		intent.setPackage(application.packageName)
		mPermissionIntent = PendingIntent.getBroadcast(
			application,
			0,
			intent,
			PendingIntent.FLAG_MUTABLE
		)

		val filter = IntentFilter().apply {
			addAction(ACTION_USB_PERMISSION)
			addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
			addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
			application.registerReceiver(mUsbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
		else
			application.registerReceiver(mUsbReceiver, filter)

		mMidiUsbRegistered = true

		attemptUsbMidiConnection()
		mInitialised = true
	}

	fun removeReceiver(task: ReceiverTask) {
		mReceiverTasks.stopAndRemoveReceiver(task.mName)
	}

	class MidiNativeDeviceListener : MidiManager.OnDeviceOpenedListener,
		MidiManager.DeviceCallback() {
		override fun onDeviceAdded(deviceInfo: MidiDeviceInfo) {
			addNativeDevice(deviceInfo)
		}

		override fun onDeviceRemoved(deviceInfo: MidiDeviceInfo) {
			deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)?.also {
				mSenderTask.removeSender(it)
				mReceiverTasks.stopAndRemoveReceiver(it)
			}
		}

		override fun onDeviceOpened(openedDevice: MidiDevice?) {
			try {
				openedDevice?.apply {
					info.properties.getString(MidiDeviceInfo.PROPERTY_NAME)?.also { deviceName ->
						info.ports.forEach {
							if (it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT)
								mReceiverTasks.addReceiver(
									deviceName,
									deviceName,
									NativeReceiver(openedDevice.openOutputPort(it.portNumber), deviceName)
								)
							else if (it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT)
								mSenderTask.addSender(
									deviceName,
									NativeSender(openedDevice.openInputPort(it.portNumber), deviceName)
								)
						}
						EventRouter.sendEventToSongList(Events.CONNECTION_ADDED, deviceName)
					}
				}
			} catch (ioException: Exception) {
				// Obviously not for us.
			}
		}
	}

	internal fun addBeatClockMessages(amount: Int) {
		if (mInitialised)
			mMIDIOutQueue.addBeatClockMessages(amount)
	}

	internal fun putMessage(message: OutgoingMessage) {
		if (mInitialised)
			mMIDIOutQueue.putMessage(message)
	}

	internal fun putMessages(messages: List<OutgoingMessage>) {
		if (mInitialised)
			mMIDIOutQueue.putMessages(messages)
	}
}
