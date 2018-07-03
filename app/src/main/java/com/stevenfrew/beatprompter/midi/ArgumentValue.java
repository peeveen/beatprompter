package com.stevenfrew.beatprompter.midi;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;

class ArgumentValue extends Value {
    private int mArgumentIndex;
    ArgumentValue(int index)
    {
        mArgumentIndex=index;
    }

    @Override
    byte resolve(byte[] arguments, byte channel) throws ResolutionException {
        if(mArgumentIndex>=arguments.length)
            throw new ResolutionException(BeatPrompterApplication.getResourceString(R.string.not_enough_parameters_supplied));
        return arguments[mArgumentIndex];
    }

    @Override
    boolean matches(Value otherValue) {
        if(otherValue instanceof ArgumentValue)
            return (((ArgumentValue) otherValue).mArgumentIndex==mArgumentIndex);
        return otherValue instanceof WildcardValue;
    }
}
