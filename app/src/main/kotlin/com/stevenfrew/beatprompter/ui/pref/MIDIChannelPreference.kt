package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import androidx.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import android.widget.GridLayout
import android.widget.ToggleButton
import com.stevenfrew.beatprompter.R

class MIDIChannelPreference : DialogPreference, CompoundButton.OnCheckedChangeListener {
    private var mCurrentValue: Int = 0
    private val mSingleSelect: Boolean
    private var mView: View? = null

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        dialogLayoutResource = R.layout.midi_channel_preference_dialog
        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)
        mSingleSelect = getSingleSelectSetting(attrs)
        mCurrentValue = if (mSingleSelect) 1 else 65535
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        dialogLayoutResource = R.layout.midi_channel_preference_dialog
        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)
        mSingleSelect = getSingleSelectSetting(attrs)
        mCurrentValue = if (mSingleSelect) 1 else 65535
    }


    private fun getSingleSelectSetting(attrs: AttributeSet): Boolean {
        val resourceID = attrs.getAttributeResourceValue(sfns, "singleSelect", 0)
        return if (resourceID == 0)
            attrs.getAttributeBooleanValue(sfns, "singleSelect", false)
        else
            context.getString(resourceID).toBoolean()
    }
/*
    override fun onBindDialogViewHolder(view: PreferenceView) {
        super.onBindDialogView(view)
        mView = view
        view.findViewById<GridLayout>(R.id.midiGrid).apply {
            useDefaultMargins = false
            alignmentMode = GridLayout.ALIGN_BOUNDS
            isRowOrderPreserved = false
        }
        mCurrentValue = this.getPersistedInt(if (mSingleSelect) 1 else 65535)
        repeat(16) {
            val tb = view.findViewById<ToggleButton>(toggleIDs[it])
            val set = mCurrentValue and (1 shl it) != 0
            tb.isChecked = set
            if (mSingleSelect && set)
                tb.isEnabled = false
            tb.setOnCheckedChangeListener(this)
        }
    }
*/
    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
        if (restorePersistedValue) {
            // Restore existing state
            mCurrentValue = this.getPersistedInt(if (mSingleSelect) 1 else 65535)
        } else {
            // Set default state from the XML attribute
            mCurrentValue = defaultValue as Int
            persistInt(mCurrentValue)
        }
    }
/*
    override fun onDialogClosed(positiveResult: Boolean) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            persistInt(mCurrentValue)
        }
    }
*/
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
        private const val sfns = "http://com.stevenfrew/"
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
    }
}
