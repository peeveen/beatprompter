package com.stevenfrew.beatprompter.pref;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.Utils;

import java.util.Locale;

public class FontSizePreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {

    private SeekBar mSeekBar;
    private TextView mTextView;

    public static int FONT_SIZE_OFFSET;
    public static int FONT_SIZE_MAX;
    public static int FONT_SIZE_MIN;
    private int mCurrentValue=-1;

    @SuppressWarnings("unused")
    public FontSizePreference(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        setDialogLayoutResource(R.layout.font_size_preference_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @SuppressWarnings("unused")
    public FontSizePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.font_size_preference_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
    }

    @Override
    protected void onBindDialogView(View view)
    {
        mSeekBar = view.findViewById(R.id.fontSizeSeekBar);
        mTextView = view.findViewById(R.id.fontSizeTextView);
        mSeekBar.setOnSeekBarChangeListener(this);
        mCurrentValue=getPersistedInt(FONT_SIZE_MIN);
        if(mCurrentValue<=0) {
            String prefKey = this.getKey();
            if(prefKey.equals(BeatPrompterApplication.getResourceString(R.string.pref_maxFontSize_key)))
                mCurrentValue=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_maxFontSize_default));
            else if(prefKey.equals(BeatPrompterApplication.getResourceString(R.string.pref_minFontSize_key)))
                mCurrentValue=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_minFontSize_default));
            else if(prefKey.equals(BeatPrompterApplication.getResourceString(R.string.pref_maxFontSizeSmooth_key)))
                mCurrentValue=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_maxFontSizeSmooth_default));
            else
                mCurrentValue=Integer.parseInt(BeatPrompterApplication.getResourceString(R.string.pref_minFontSizeSmooth_default));
        }
        mSeekBar.setProgress(mCurrentValue);
        mTextView.setText(String.format(Locale.getDefault(),"%d",mCurrentValue));
        mSeekBar.setMax(FONT_SIZE_MAX);
        super.onBindDialogView(view);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            mCurrentValue=this.getPersistedInt(FONT_SIZE_MIN);
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
            persistInt(mSeekBar.getProgress());
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int size=(mCurrentValue=progress)+FONT_SIZE_OFFSET;
        mTextView.setText(String.format(Locale.getDefault(),"%d",size));
        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,size* Utils.FONT_SCALING);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
