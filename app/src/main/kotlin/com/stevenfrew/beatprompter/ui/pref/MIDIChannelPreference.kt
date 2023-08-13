package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.content.res.TypedArray
import androidx.preference.DialogPreference
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import android.widget.GridLayout
import android.widget.ToggleButton
import com.stevenfrew.beatprompter.R

class MIDIChannelPreference : DialogPreference {
	private var mDefaultValue:Int=0
  val singleSelect: Boolean

	constructor(context: Context?):	this(context, null)

	constructor(context: Context?, attrs: AttributeSet?):this(context, attrs, 0)

	constructor(	context: Context?, attrs: AttributeSet?,	defStyleAttr: Int	):this(context, attrs, defStyleAttr, defStyleAttr)

	constructor(
		context: Context?, attrs: AttributeSet?,
		defStyleAttr: Int, defStyleRes: Int
	) :	super(context!!, attrs, defStyleAttr, defStyleRes){
		singleSelect = attrs?.getAttributeBooleanValue(sfns, "singleSelect", false) ?: false
	}

	fun setPreferenceValue(fontSize:Int) {
		persistInt(fontSize)
	}

	fun getPreferenceValue():Int{
		return getPersistedInt(mDefaultValue)
	}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
		mDefaultValue=a.getString(index)!!.toInt()
		return mDefaultValue
	}

    override fun onSetInitialValue(defaultValue: Any?) {
				if(defaultValue is Int)
					setPreferenceValue(defaultValue)
    }

    companion object {
			private const val sfns = "http://com.stevenfrew/"
		}
}
