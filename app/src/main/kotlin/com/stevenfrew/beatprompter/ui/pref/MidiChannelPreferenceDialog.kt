package com.stevenfrew.beatprompter.ui.pref

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.GridLayout
import android.widget.ToggleButton
import androidx.preference.PreferenceDialogFragmentCompat
import com.stevenfrew.beatprompter.R

class MidiChannelPreferenceDialog(private val singleSelect: Boolean) :
	PreferenceDialogFragmentCompat(), CompoundButton.OnCheckedChangeListener {
	private var currentValue: Int = 0
	private var gridLayoutView: GridLayout? = null
	override fun onBindDialogView(view: View) {
		gridLayoutView = view.findViewById(R.id.midiGrid)
		gridLayoutView!!.apply {
			useDefaultMargins = false
			alignmentMode = GridLayout.ALIGN_BOUNDS
			isRowOrderPreserved = false
		}
		currentValue = (this.preference as MidiChannelPreference).channelMask
		repeat(16) {
			val tb = view.findViewById<ToggleButton>(toggleIDs[it])
			val set = currentValue and (1 shl it) != 0
			tb.isChecked = set
			if (singleSelect && set)
				tb.isEnabled = false
			tb.setOnCheckedChangeListener(this)
		}
		super.onBindDialogView(view)
	}

	override fun onDialogClosed(positiveResult: Boolean) {
		if (positiveResult)
			(this.preference as MidiChannelPreference).channelMask = currentValue
	}

	override fun onCheckedChanged(buttonView: CompoundButton, isNowChecked: Boolean) {
		if (singleSelect && isNowChecked) {
			repeat(16) {
				gridLayoutView!!.findViewById<ToggleButton>(toggleIDs[it]).apply {
					if (this !== buttonView) {
						if (isChecked && !isEnabled) {
							isChecked = false
							isEnabled = true
						}
					}
				}
			}
			buttonView.isEnabled = false
		}
		repeat(16) {
			if (toggleIDs[it] == buttonView.id) {
				currentValue = if (isNowChecked)
					currentValue or (1 shl it)
				else
					currentValue and (1 shl it).inv()
				return
			}
		}
	}

	companion object {
		private val toggleIDs = intArrayOf(
			R.id.midiChannel1Button,
			R.id.midiChannel2Button,
			R.id.midiChannel3Button,
			R.id.midiChannel4Button,
			R.id.midiChannel5Button,
			R.id.midiChannel6Button,
			R.id.midiChannel7Button,
			R.id.midiChannel8Button,
			R.id.midiChannel9Button,
			R.id.midiChannel10Button,
			R.id.midiChannel11Button,
			R.id.midiChannel12Button,
			R.id.midiChannel13Button,
			R.id.midiChannel14Button,
			R.id.midiChannel15Button,
			R.id.midiChannel16Button
		)

		fun newInstance(
			key: String?,
			singleSelect: Boolean
		): MidiChannelPreferenceDialog {
			val fragment = MidiChannelPreferenceDialog(singleSelect)
			val b = Bundle(1)
			b.putString(ARG_KEY, key)
			fragment.arguments = b
			return fragment
		}
	}
}
