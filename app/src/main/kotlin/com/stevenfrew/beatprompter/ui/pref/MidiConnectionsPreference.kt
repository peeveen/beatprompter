package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.preference.MultiSelectListPreference
import androidx.preference.PreferenceViewHolder
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R

class MidiConnectionsPreference(context: Context, attrs: AttributeSet) :
	MultiSelectListPreference(context, attrs) {

	override fun onBindViewHolder(view: PreferenceViewHolder) {
		super.onBindViewHolder(view)
		val usbImageView = view.findViewById(R.id.midiUsbOnTheGoIconImageView) as ImageView
		val nativeImageView = view.findViewById(R.id.midiNativeIconImageView) as ImageView
		val bluetoothImageView = view.findViewById(R.id.midiBluetoothIconImageView) as ImageView
		val currentPrefValue = Preferences.getStringSetPreference(
			key,
			BeatPrompter.appResources.getStringSet(R.array.pref_midiConnectionTypes_defaultValues)
		)
		val usbEnabled =
			currentPrefValue.contains(BeatPrompter.appResources.getString(R.string.midi_usb_on_the_go_value))
		val nativeEnabled =
			currentPrefValue.contains(BeatPrompter.appResources.getString(R.string.midi_native_value))
		val bluetoothEnabled =
			currentPrefValue.contains(BeatPrompter.appResources.getString(R.string.midi_bluetooth_value))
		usbImageView.visibility = if (usbEnabled) View.VISIBLE else View.GONE
		nativeImageView.visibility = if (nativeEnabled) View.VISIBLE else View.GONE
		bluetoothImageView.visibility = if (bluetoothEnabled) View.VISIBLE else View.GONE
	}
}
