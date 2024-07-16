package com.stevenfrew.beatprompter.ui.pref

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import com.stevenfrew.beatprompter.comm.bluetooth.Bluetooth

class BandLeaderPreference(context: Context, attrs: AttributeSet) : ListPreference(context, attrs) {
	private val mBluetoothDevices: List<BluetoothDevice> = Bluetooth.getPairedDevices()

	override fun getEntries(): Array<CharSequence> {
		return mBluetoothDevices.map { it.name }.toTypedArray()
	}

	override fun getEntryValues(): Array<CharSequence> {
		return mBluetoothDevices.map { it.address }.toTypedArray()
	}
}