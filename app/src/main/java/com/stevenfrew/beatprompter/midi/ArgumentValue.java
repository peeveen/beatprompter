package com.stevenfrew.beatprompter.midi;

import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;

class ArgumentValue extends Value {
    private int mArgumentIndex;
    ArgumentValue(int index)
    {
        mArgumentIndex=index;
    }

    @Override
    byte resolve(byte[] arguments, byte channel) throws ResolutionException {
        if(mArgumentIndex>=arguments.length)
            throw new ResolutionException(SongList.mSongListInstance.getString(R.string.not_enough_parameters_supplied));
        return arguments[mArgumentIndex];
    }

    @Override
    boolean matches(Value otherValue) {
        if(otherValue instanceof ArgumentValue)
            return (((ArgumentValue) otherValue).mArgumentIndex==mArgumentIndex);
        return otherValue instanceof WildcardValue;
    }
}
