package com.stevenfrew.beatprompter.pref;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.GridLayout;
import android.widget.ToggleButton;

import com.stevenfrew.beatprompter.R;

public class MIDIChannelPreference extends DialogPreference implements CompoundButton.OnCheckedChangeListener {
    private int mCurrentValue;
    private boolean mSingleSelect;
    private View mView;

    private static final String sfns="http://com.stevenfrew/";

    @SuppressWarnings("unused")
    public MIDIChannelPreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        setDialogLayoutResource(R.layout.midi_channel_preference_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        mSingleSelect = getSingleSelectSetting(attrs);
        mCurrentValue=mSingleSelect?1:65535;
    }

    @SuppressWarnings("unused")
    public MIDIChannelPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.midi_channel_preference_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        mSingleSelect = getSingleSelectSetting(attrs);
        mCurrentValue=mSingleSelect?1:65535;
    }


    private boolean getSingleSelectSetting(AttributeSet attrs)
    {
        int resourceID=attrs.getAttributeResourceValue(sfns, "singleSelect",0);
        if(resourceID==0)
            return attrs.getAttributeBooleanValue(sfns, "singleSelect", false);
        return Boolean.parseBoolean(getContext().getString(resourceID));

    }

    private static int toggleIDs[]=new int[]
            {
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
                    R.id.midiChannel16Button
            };

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mView=view;
        GridLayout gridLayout = view.findViewById(R.id.midiGrid);
        gridLayout.setUseDefaultMargins(false);
        gridLayout.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        gridLayout.setRowOrderPreserved(false);
        mCurrentValue=this.getPersistedInt(mSingleSelect?1:65535);
        for(int f=0;f<16;++f) {
            ToggleButton tb=view.findViewById(toggleIDs[f]);
            boolean set=(mCurrentValue & (1 << f)) != 0;
            tb.setChecked(set);
            if((mSingleSelect)&&(set))
                tb.setEnabled(false);
            tb.setOnCheckedChangeListener(this);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            mCurrentValue=this.getPersistedInt(mSingleSelect?1:65535);
        } else {
            // Set default state from the XML attribute
            mCurrentValue=(Integer) defaultValue;
            persistInt(mCurrentValue);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            persistInt(mCurrentValue);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if((mSingleSelect)&&(isChecked))
        {
            for(int f=0;f<16;++f) {
                ToggleButton tb=mView.findViewById(toggleIDs[f]);
                if(tb!=buttonView)
                {
                    if ((tb.isChecked())&&(!tb.isEnabled()))
                    {
                        tb.setChecked(false);
                        tb.setEnabled(true);
                    }
                }
            }
            buttonView.setEnabled(false);
        }
        for(int f=0;f<16;++f) {
            if (toggleIDs[f] == buttonView.getId()) {
                if (isChecked)
                    mCurrentValue |= (1 << f);
                else
                    mCurrentValue &= ~(1 << f);
                break;
            }
        }
    }
}
