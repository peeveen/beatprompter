package com.stevenfrew.beatprompter.ui.pref

import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.GridLayout
import android.widget.ToggleButton
import androidx.preference.PreferenceDialogFragmentCompat
import com.stevenfrew.beatprompter.R

class MIDIChannelPreferenceDialog(private val mSingleSelect:Boolean) : PreferenceDialogFragmentCompat(), CompoundButton.OnCheckedChangeListener {
		private var mCurrentValue:Int=0
		private var mView:GridLayout?=null
		override fun onBindDialogView(view: View) {
				mView=view.findViewById<GridLayout>(R.id.midiGrid)
				mView!!.apply {
						useDefaultMargins = false
						alignmentMode = GridLayout.ALIGN_BOUNDS
						isRowOrderPreserved = false
				}
				mCurrentValue = (this.preference as MIDIChannelPreference).getPreferenceValue()
				repeat(16) {
						val tb = view.findViewById<ToggleButton>(toggleIDs[it])
						val set = mCurrentValue and (1 shl it) != 0
						tb.isChecked = set
						if (mSingleSelect && set)
								tb.isEnabled = false
						tb.setOnCheckedChangeListener(this)
				}
			super.onBindDialogView(view)
		}

	override fun onDialogClosed(positiveResult: Boolean) {
		if (positiveResult) {
			(this.preference as MIDIChannelPreference).setPreferenceValue(mCurrentValue)
		}
	}

	override fun onCheckedChanged(buttonView: CompoundButton, isNowChecked: Boolean) {
		if (mSingleSelect && isNowChecked) {
			repeat(16) {
				mView!!.findViewById<ToggleButton>(toggleIDs[it]).apply {
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
				mCurrentValue = if (isNowChecked)
					mCurrentValue or (1 shl it)
				else
					mCurrentValue and (1 shl it).inv()
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
			R.id.midiChannel16Button)

		fun newInstance(
			key: String?,
			singleSelect:Boolean
		): MIDIChannelPreferenceDialog {
			val fragment = MIDIChannelPreferenceDialog(singleSelect)
			val b = Bundle(1)
			b.putString(ARG_KEY, key)
			fragment.arguments = b
			return fragment
		}
	}
}
