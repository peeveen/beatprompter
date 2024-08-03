package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.Message
import com.stevenfrew.beatprompter.comm.MessageQueue
import com.stevenfrew.beatprompter.comm.ReceiverTask
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask

/**
 * General Bluetooth management singleton object.
 */
object Bluetooth {
	private var initialised = false

	private const val BLUETOOTH_QUEUE_SIZE = 4096
	private val bluetoothOutQueue = MessageQueue(BLUETOOTH_QUEUE_SIZE)
	private val senderTask = SenderTask(bluetoothOutQueue)
	private val receiverTasks = ReceiverTasks()
	private val senderTaskThread = Thread(senderTask)

	/**
	 * Called when the app starts. Doing basic Bluetooth setup.
	 */
	fun initialize(context: Context) {
		senderTaskThread.start()
		Task.resumeTask(senderTask, senderTaskThread)

		BandBluetoothController.initialize(context, senderTask, receiverTasks)

		initialised = true
	}

	fun getBluetoothAdapter(context: Context): BluetoothAdapter? =
		if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
			val bluetoothManager =
				context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
			bluetoothManager.adapter
		} else null

	fun getPairedDevices(context: Context): List<BluetoothDevice> =
		getPairedDevices(getBluetoothAdapter(context))

	fun getPairedDevices(bluetoothAdapter: BluetoothAdapter?): List<BluetoothDevice> =
		try {
			bluetoothAdapter?.bondedDevices?.toList() ?: listOf()
		} catch (se: SecurityException) {
			Logger.logComms("A Bluetooth security exception was thrown while getting paired devices.", se)
			listOf()
		}

	/**
	 * Do we have a connection to a band leader?
	 */
	val isConnectedToServer: Boolean
		get() = receiverTasks.taskCount > 0

	/**
	 * As a band leader, how many band members are we connected to?
	 */
	val bluetoothClientCount: Int
		get() = senderTask.senderCount

	internal fun putMessage(message: Message) {
		if (initialised) bluetoothOutQueue.putMessage(message)
	}

	fun removeReceiver(task: ReceiverTask) = receiverTasks.stopAndRemoveReceiver(task.name)
}
