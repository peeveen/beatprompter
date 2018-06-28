package com.stevenfrew.beatprompter.midi;

public class ArgumentValue extends Value {
    int mArgumentIndex;
    ArgumentValue(int index)
    {
        mArgumentIndex=index;
    }

    @Override
    byte resolve(byte[] arguments, byte channel) throws ResolutionException {
        if((mArgumentIndex<0)||(mArgumentIndex>=arguments.length))
            throw new ResolutionException();
        return arguments[mArgumentIndex];
    }

    @Override
    boolean matches(Value otherValue) {
        if(otherValue instanceof ArgumentValue)
            return (((ArgumentValue) otherValue).mArgumentIndex==mArgumentIndex);
        return otherValue instanceof WildcardValue;
    }
}
