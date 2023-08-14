package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference

class FontSizePreference : DialogPreference {
	private var mDefaultValue: Int = 0

	constructor(context: Context?) : this(context, null)

	constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

	constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : this(
		context,
		attrs,
		defStyleAttr,
		defStyleAttr
	)

	constructor(
		context: Context?, attrs: AttributeSet?,
		defStyleAttr: Int, defStyleRes: Int
	) : super(context!!, attrs, defStyleAttr, defStyleRes)

	override fun onSetInitialValue(defaultValue: Any?) {
		// Set default state from the XML attribute
		if (defaultValue is Int)
			setFontSize(defaultValue)
	}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
		mDefaultValue = a.getString(index)!!.toInt()
		return mDefaultValue
	}

	fun setFontSize(fontSize: Int) {
		persistInt(fontSize)
	}

	fun getFontSize(): Int {
		return getPersistedInt(mDefaultValue)
	}

	companion object {
		// Set by onCreate() in SongListActivity.java
		var FONT_SIZE_OFFSET: Int = 0
		var FONT_SIZE_MAX: Int = 0
		var FONT_SIZE_MIN: Int = 0
	}
}
