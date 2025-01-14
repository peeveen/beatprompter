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

	private var defaultValue: Int = 0
	val singleSelect: Boolean =
		attrs?.getAttributeBooleanValue(
			SettingsFragment.STEVEN_FREW_NAMESPACE,
			"singleSelect",
			false
		) == true

	var channelMask: Int
		get() = getPersistedInt(defaultValue)
		set(value) {
			persistInt(value)
		}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
		defaultValue = a.getString(index)!!.toInt()
		return defaultValue
	}

	override fun onSetInitialValue(defaultValue: Any?) {
		if (defaultValue is Int)
			channelMask = defaultValue
	}
}
