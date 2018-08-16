package com.stevenfrew.beatprompter.pref

import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import android.widget.GridLayout
import android.widget.ToggleButton
import com.stevenfrew.beatprompter.R

class MIDIChannelPreference : DialogPreference, CompoundButton.OnCheckedChangeListener {
    private var mCurrentValue: Int = 0
    private var mSingleSelect: Boolean = false
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
        return if (resourceID == 0) attrs.getAttributeBooleanValue(sfns, "singleSelect", false) else context.getString(resourceID).toBoolean()

    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)
        mView = view
        val gridLayout = view.findViewById<GridLayout>(R.id.midiGrid)
        gridLayout.useDefaultMargins = false
        gridLayout.alignmentMode = GridLayout.ALIGN_BOUNDS
        gridLayout.isRowOrderPreserved = false
        mCurrentValue = this.getPersistedInt(if (mSingleSelect) 1 else 65535)
        for (f in 0..15) {
            val tb = view.findViewById<ToggleButton>(toggleIDs[f])
            val set = mCurrentValue and (1 shl f) != 0
            tb.isChecked = set
            if (mSingleSelect && set)
                tb.isEnabled = false
            tb.setOnCheckedChangeListener(this)
        }
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any) {
        if (restorePersistedValue) {
            // Restore existing state
            mCurrentValue = this.getPersistedInt(if (mSingleSelect) 1 else 65535)
        } else {
            // Set default state from the XML attribute
            mCurrentValue = defaultValue as Int
            persistInt(mCurrentValue)
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            persistInt(mCurrentValue)
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        if (mSingleSelect && isChecked) {
            for (f in 0..15) {
                val tb = mView!!.findViewById<ToggleButton>(toggleIDs[f])
                if (tb !== buttonView) {
                    if (tb.isChecked && !tb.isEnabled) {
                        tb.isChecked = false
                        tb.isEnabled = true
                    }
                }
            }
            buttonView.isEnabled = false
        }
        for (f in 0..15) {
            if (toggleIDs[f] == buttonView.id) {
                mCurrentValue = if (isChecked)
                    mCurrentValue or (1 shl f)
                else
                    mCurrentValue and (1 shl f).inv()
                break
            }
        }
    }

    companion object {

        private const val sfns = "http://com.stevenfrew/"

        private val toggleIDs = intArrayOf(R.id.midiChannel1Button, R.id.midiChannel2Button, R.id.midiChannel3Button, R.id.midiChannel4Button, R.id.midiChannel5Button, R.id.midiChannel6Button, R.id.midiChannel7Button, R.id.midiChannel8Button, R.id.midiChannel9Button, R.id.midiChannel10Button, R.id.midiChannel11Button, R.id.midiChannel12Button, R.id.midiChannel13Button, R.id.midiChannel14Button, R.id.midiChannel15Button, R.id.midiChannel16Button)
    }
}
