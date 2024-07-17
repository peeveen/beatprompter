package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference

class FontSizePreference(
	context: Context?,
	attrs: AttributeSet?,
	defStyleAttr: Int,
	defStyleRes: Int
) : DialogPreference(context!!, attrs, defStyleAttr, defStyleRes) {
	// Don't be fooled by the IDE. This constructor is REQUIRED!!!
	constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0, 0)

	private var mDefaultValue: Int = 0

	override fun onSetInitialValue(defaultValue: Any?) {
		// Set default state from the XML attribute
		if (defaultValue is Int)
			fontSize = defaultValue
	}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
		mDefaultValue = a.getString(index)!!.toInt()
		return mDefaultValue
	}

	var fontSize: Int
		get() = getPersistedInt(mDefaultValue)
		set(value) {
			persistInt(value)
		}

	companion object {
		// Set by onCreate() in SongListActivity.java
		var FONT_SIZE_OFFSET: Int = 0
		var FONT_SIZE_MAX: Int = 0
		var FONT_SIZE_MIN: Int = 0
	}
}
