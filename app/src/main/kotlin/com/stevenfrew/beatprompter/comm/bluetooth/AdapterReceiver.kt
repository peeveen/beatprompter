package com.stevenfrew.beatprompter.comm.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * User could switch Bluetooth functionality on/off at any time.
 * We need to keep an eye on that.
 */
abstract class AdapterReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
			when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
				BluetoothAdapter.STATE_TURNING_OFF -> onBluetoothDisabled()
				BluetoothAdapter.STATE_ON -> onBluetoothEnabled(context)
			}
		}
	}

	abstract fun onBluetoothDisabled()
	abstract fun onBluetoothEnabled(context: Context)
}
