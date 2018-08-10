package com.stevenfrew.beatprompter.pref

import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.Utils
import java.util.*

class FontSizePreference : DialogPreference, SeekBar.OnSeekBarChangeListener {

    private var mSeekBar: SeekBar? = null
    private var mTextView: TextView? = null
    private var mCurrentValue = -1

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        dialogLayoutResource = R.layout.font_size_preference_dialog
        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        dialogLayoutResource = R.layout.font_size_preference_dialog
        setPositiveButtonText(android.R.string.ok)
        setNegativeButtonText(android.R.string.cancel)
    }

    override fun onBindDialogView(view: View) {
        mSeekBar = view.findViewById(R.id.fontSizeSeekBar)
        mTextView = view.findViewById(R.id.fontSizeTextView)
        mSeekBar!!.setOnSeekBarChangeListener(this)
        mCurrentValue = getPersistedInt(FONT_SIZE_MIN)
        if (mCurrentValue <= 0) {
            val prefKey = this.key
            mCurrentValue = if (prefKey == BeatPrompterApplication.getResourceString(R.string.pref_maxFontSize_key))
                Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_maxFontSize_default))
            else if (prefKey == BeatPrompterApplication.getResourceString(R.string.pref_minFontSize_key))
                Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_minFontSize_default))
            else if (prefKey == BeatPrompterApplication.getResourceString(R.string.pref_maxFontSizeSmooth_key))
                Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_maxFontSizeSmooth_default))
            else
                Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_minFontSizeSmooth_default))
        }
        mSeekBar!!.progress = mCurrentValue
        mTextView!!.text = String.format(Locale.getDefault(), "%d", mCurrentValue)
        mSeekBar!!.max = FONT_SIZE_MAX
        super.onBindDialogView(view)
    }

    override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any) {
        if (restorePersistedValue) {
            // Restore existing state
            mCurrentValue = this.getPersistedInt(FONT_SIZE_MIN)
        } else {
            // Set default state from the XML attribute
            mCurrentValue = defaultValue as Int
            persistInt(mCurrentValue)
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            persistInt(mSeekBar!!.progress)
        }
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        mCurrentValue = progress
        val size =  mCurrentValue + FONT_SIZE_OFFSET
        mTextView!!.text = String.format(Locale.getDefault(), "%d", size)
        mTextView!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * Utils.FONT_SCALING)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}

    override fun onStopTrackingTouch(seekBar: SeekBar) {}

    companion object {

        @JvmField var FONT_SIZE_OFFSET: Int = 0
        @JvmField var FONT_SIZE_MAX: Int = 0
        @JvmField var FONT_SIZE_MIN: Int = 0
    }
}
