package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference

class MidiChannelPreference(
	context: Context?,
	attrs: AttributeSet?,
	defStyleAttr: Int,
	defStyleRes: Int
) : DialogPreference(context!!, attrs, defStyleAttr, defStyleRes) {
	// Don't be fooled by the IDE. This constructor is REQUIRED!!!
	constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0, 0)

	private var mDefaultValue: Int = 0
	val singleSelect: Boolean =
		attrs?.getAttributeBooleanValue(SettingsFragment.STEVEN_FREW_NAMESPACE, "singleSelect", false)
			?: false

	var channelMask: Int
		get() = getPersistedInt(mDefaultValue)
		set(value) {
			persistInt(value)
		}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
		mDefaultValue = a.getString(index)!!.toInt()
		return mDefaultValue
	}

	override fun onSetInitialValue(defaultValue: Any?) {
		if (defaultValue is Int)
			channelMask = defaultValue
	}
}
