package com.stevenfrew.beatprompter;

import java.util.ArrayList;

class MIDIAliasParameter
{
    private boolean mIsParameterReference=false;
    private MIDIValue mValue;
    private int mIndex;
    private boolean mMergeWithChannel=false;

    private MIDIAliasParameter(byte value,boolean mergeWithChannel)
    {
        mValue=new MIDIValue(value);
        mMergeWithChannel=mergeWithChannel;
    }
    MIDIAliasParameter(String parameter)
    {
        if(parameter.startsWith("?"))
        {
            parameter=parameter.substring(1);
            mIndex=Integer.parseInt(parameter);
            mIsParameterReference=true;
        }
        else
        {
            if(parameter.contains("_")) {
                parameter = parameter.replace('_', '0');
                mMergeWithChannel=true;
            }
            if(parameter.contains("_"))
                throw new IllegalArgumentException(SongList.getContext().getString(R.string.multiple_underscores_in_midi_value));
            mValue=MIDIMessage.parseValue(parameter);
            if (mMergeWithChannel) {
                if(mValue.mChannelSpecifier)
                    throw new IllegalArgumentException(SongList.getContext().getString(R.string.channel_specifier_cannot_merge_with_channel_specifier));
                if (!MIDIMessage.looksLikeHex(parameter))
                    throw new IllegalArgumentException(SongList.getContext().getString(R.string.underscore_in_decimal_value));
                if((mValue.mValue&0x0F)!=0)
                    throw new IllegalArgumentException(SongList.getContext().getString(R.string.merge_with_channel_non_zero_lower_nibble));
            }
        }
    }
    byte getValue(MIDIValue[] parameters,byte channel)
    {
        byte val;
        if(mIsParameterReference)
            val=parameters[mIndex-1].mValue;
        else
            val=mValue.mValue;
        if(mMergeWithChannel)
            return mergeWithChannel(val,channel);
        return val;
    }
    MIDIAliasParameter substitute(ArrayList<MIDIAliasParameter> values)
    {
        if(mIsParameterReference)
            if(values.size()>=mIndex)
                return values.get(mIndex-1);
            else
                throw new IllegalArgumentException(SongList.getContext().getString(R.string.not_enough_parameters_supplied));
        else if(mergesWithChannel()) {
            if (values.size() > 0)
                if(values.get(values.size() - 1).mValue!=null)
                    if (values.get(values.size() - 1).mValue.mChannelSpecifier)
                        return new MIDIAliasParameter(mergeWithChannel(mValue.mValue, values.get(values.size() - 1).mValue.mValue),false);
        }
        return new MIDIAliasParameter(mValue.mValue,mMergeWithChannel);
    }
    int getParameterIndexReference()
    {
        return mIndex;
    }
    private boolean mergesWithChannel()
    {
        return mMergeWithChannel;
    }
    private static byte mergeWithChannel(byte value,byte channel)
    {
        return (byte) (value | channel);
    }
    boolean isChannelReference() {
        return !mIsParameterReference && mValue.mChannelSpecifier;
    }
}
