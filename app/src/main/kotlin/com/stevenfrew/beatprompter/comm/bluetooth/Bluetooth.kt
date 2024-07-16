package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.stevenfrew.beatprompter.Task
import com.stevenfrew.beatprompter.comm.MessageQueue
import com.stevenfrew.beatprompter.comm.OutgoingMessage
import com.stevenfrew.beatprompter.comm.ReceiverTask
import com.stevenfrew.beatprompter.comm.ReceiverTasks
import com.stevenfrew.beatprompter.comm.SenderTask
import com.stevenfrew.beatprompter.comm.bluetooth.message.HeartbeatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * General Bluetooth management singleton object.
 */
object Bluetooth : CoroutineScope {
	private val mCoRoutineJob = Job()
	override val coroutineContext: CoroutineContext
		get() = Dispatchers.Default + mCoRoutineJob

	private var mInitialised = false

	private const val BLUETOOTH_QUEUE_SIZE = 4096
	private val mBluetoothOutQueue = MessageQueue(BLUETOOTH_QUEUE_SIZE)
	private val mSenderTask = SenderTask(mBluetoothOutQueue)
	private val mReceiverTasks = ReceiverTasks()
	private val mSenderTaskThread = Thread(mSenderTask)
	private var mController: BluetoothController? = null

	/**
	 * Called when the app starts. Doing basic Bluetooth setup.
	 */
	fun initialize(context: Context) {
		mSenderTaskThread.start()
		Task.resumeTask(mSenderTask)

		mController = BluetoothController(context, mSenderTask, mReceiverTasks)
		if (mController?.isActive == true) {
			launch {
				while (true) {
					mBluetoothOutQueue.putMessage(HeartbeatMessage)
					delay(1000)
				}
			}
		}

		mInitialised = true
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

	fun getPairedDevices(): List<BluetoothDevice> {
		return mController?.getPairedDevices() ?: listOf()
	}

	internal fun putMessage(message: OutgoingMessage) {
		if (mInitialised) mBluetoothOutQueue.putMessage(message)
	}

	fun removeReceiver(task: ReceiverTask) {
		mReceiverTasks.stopAndRemoveReceiver(task.mName)
	}
}
