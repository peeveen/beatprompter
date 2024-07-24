package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import com.stevenfrew.beatprompter.Logger
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.MessageQueue
import com.stevenfrew.beatprompter.comm.OutgoingMessage
import com.stevenfrew.beatprompter.comm.ReceiverTask
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask

/**
 * General Bluetooth management singleton object.
 */
object Bluetooth {
	private var mInitialised = false

	private const val BLUETOOTH_QUEUE_SIZE = 4096
	private val mBluetoothOutQueue = MessageQueue(BLUETOOTH_QUEUE_SIZE)
	private val mSenderTask = SenderTask(mBluetoothOutQueue)
	private val mReceiverTasks = ReceiverTasks()
	private val mSenderTaskThread = Thread(mSenderTask)

	/**
	 * Called when the app starts. Doing basic Bluetooth setup.
	 */
	fun initialize(context: Context) {
		mSenderTaskThread.start()
		Task.resumeTask(mSenderTask)

		BandBluetoothController.initialize(context, mSenderTask, mReceiverTasks)

		mInitialised = true
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
		get() = mReceiverTasks.taskCount > 0

	/**
	 * As a band leader, how many band members are we connected to?
	 */
	val bluetoothClientCount: Int
		get() = mSenderTask.senderCount

	internal fun putMessage(message: OutgoingMessage) {
		if (mInitialised) mBluetoothOutQueue.putMessage(message)
	}

	fun removeReceiver(task: ReceiverTask) = mReceiverTasks.stopAndRemoveReceiver(task.mName)
}
