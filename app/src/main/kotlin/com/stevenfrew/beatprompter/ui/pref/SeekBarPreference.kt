package com.stevenfrew.beatprompter.ui.pref

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class SeekBarPreference
// ------------------------------------------------------------------------------------------


// ------------------------------------------------------------------------------------------
// Constructor :
(private val mContext: Context, attrs: AttributeSet) : DialogPreference(mContext, attrs), SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private var mSeekBar: SeekBar? = null
    private var mValueText: TextView? = null

    private var mDialogMessage: String? = null
    private var mSuffix: String? = null
    private val mDefault: Int
    var max: Int = 0
    private var mValue: Int = 0
    private val mOffset: Int
    var progress: Int
        get() = mValue
        set(progress) {
            mValue = progress
            if (mSeekBar != null)
                mSeekBar!!.progress = progress
        }

    init {

        // Get string value for dialogMessage :
        val mDialogMessageId = attrs.getAttributeResourceValue(androidns, "dialogMessage", 0)
        mDialogMessage = if (mDialogMessageId == 0)
            attrs.getAttributeValue(androidns, "dialogMessage")
        else
            mContext.getString(mDialogMessageId)

        // Get string value for suffix (text attribute in xml file) :
        val mSuffixId = attrs.getAttributeResourceValue(androidns, "text", 0)
        mSuffix = if (mSuffixId == 0)
            attrs.getAttributeValue(androidns, "text")
        else
            mContext.getString(mSuffixId)

        // Get default and max seekbar values :
        mDefault = getAttributeIntValue(attrs, androidns, "defaultValue", 0)
        max = getAttributeIntValue(attrs, androidns, "max", 100)
        mOffset = getAttributeIntValue(attrs, sfns, "offset", 0)
    }
    // ------------------------------------------------------------------------------------------


    private fun getAttributeIntValue(attrs: AttributeSet, namespace: String, name: String, lastResortDefault: Int): Int {
        val resourceID = attrs.getAttributeResourceValue(namespace, name, 0)
        return if (resourceID == 0) attrs.getAttributeIntValue(namespace, name, lastResortDefault) else Integer.parseInt(context.getString(resourceID))

    }

    // ------------------------------------------------------------------------------------------
    // DialogPreference methods :
    override fun onCreateDialogView(): View {

        val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        val layout = LinearLayout(mContext)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(6, 6, 6, 6)

        val splashText = TextView(mContext)
        splashText.setPadding(30, 10, 30, 10)
        if (mDialogMessage != null)
            splashText.text = mDialogMessage
        layout.addView(splashText)

        mValueText = TextView(mContext)
        mValueText!!.gravity = Gravity.CENTER_HORIZONTAL
        mValueText!!.textSize = 32f
        layout.addView(mValueText, params)

        mSeekBar = SeekBar(mContext)
        mSeekBar!!.setOnSeekBarChangeListener(this)
        layout.addView(mSeekBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        if (shouldPersist())
            mValue = getPersistedInt(mDefault)

        mSeekBar!!.max = max
        mSeekBar!!.progress = mValue

        return layout
    }

    override fun onBindDialogView(v: View) {
        super.onBindDialogView(v)
        mSeekBar!!.max = max
        mSeekBar!!.progress = mValue
    }

    override fun onSetInitialValue(restore: Boolean, defaultValue: Any?) {
        super.onSetInitialValue(restore, defaultValue)
        mValue = if (restore)
            if (shouldPersist()) getPersistedInt(mDefault) else 0
        else
            defaultValue as Int
    }
    // ------------------------------------------------------------------------------------------


    // ------------------------------------------------------------------------------------------
    // OnSeekBarChangeListener methods :
    override fun onProgressChanged(seek: SeekBar, value: Int, fromTouch: Boolean) {
        val t = (value + mOffset).toString()
        mValueText!!.text = if (mSuffix == null) t else t + (" " + mSuffix!!)
    }

    override fun onStartTrackingTouch(seek: SeekBar) {}
    override fun onStopTrackingTouch(seek: SeekBar) {}
    // ------------------------------------------------------------------------------------------


    // ------------------------------------------------------------------------------------------
    // Set the positive button listener and onClick action :
    public override fun showDialog(state: Bundle?) {

        super.showDialog(state)

        val positiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton.setOnClickListener(this)
    }

    override fun onClick(v: View) {

        if (shouldPersist()) {

            mValue = mSeekBar!!.progress
            persistInt(mValue)
            callChangeListener(mSeekBar!!.progress)
        }

        dialog.dismiss()
    }

    companion object {
        // ------------------------------------------------------------------------------------------
        // Private attributes :
        private const val androidns = "http://schemas.android.com/apk/res/android"
        private const val sfns = "http://com.stevenfrew/"
    }
    // ------------------------------------------------------------------------------------------
}